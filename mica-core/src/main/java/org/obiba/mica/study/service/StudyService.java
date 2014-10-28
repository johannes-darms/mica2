package org.obiba.mica.study.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.obiba.mica.core.domain.LocalizedString;
import org.obiba.mica.core.service.GitService;
import org.obiba.mica.study.NoSuchStudyException;
import org.obiba.mica.study.StudyStateRepository;
import org.obiba.mica.study.domain.Study;
import org.obiba.mica.study.domain.StudyState;
import org.obiba.mica.study.event.DraftStudyUpdatedEvent;
import org.obiba.mica.study.event.IndexStudiesEvent;
import org.obiba.mica.study.event.StudyPublishedEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

import static org.obiba.mica.core.domain.RevisionStatus.DRAFT;

@Service
@Validated
public class StudyService implements ApplicationListener<ContextRefreshedEvent> {

  //private static final Logger log = LoggerFactory.getLogger(StudyService.class);

  @Inject
  private StudyStateRepository studyStateRepository;

  @Inject
  private GitService gitService;

  @Inject
  private EventBus eventBus;

  @Inject
  private ObjectMapper objectMapper;

  @CacheEvict(value = "studies-draft", key = "#study.id")
  public void save(@NotNull @Valid Study study) {
    StudyState studyState = findStudyState(study);
    gitService.save(study);

    studyState.setName(study.getName());
    studyState.incrementRevisionsAhead();
    studyStateRepository.save(studyState);

    eventBus.post(new DraftStudyUpdatedEvent(study));
  }

  @NotNull
  private StudyState findStudyState(Study study) {
    StudyState studyState;

    if(study.isNew()) {
      studyState = new StudyState();
      studyState.setName(study.getName());
      studyState.setId(generateId(study));
      studyStateRepository.save(studyState);
      study.setId(studyState.getId());
      return studyState;
    }

    studyState = studyStateRepository.findOne(study.getId());
    if(studyState == null) {
      studyState = new StudyState();
      studyState.setName(study.getName());
      studyState.setId(study.getId());
      studyStateRepository.save(studyState);
    }

    return studyState;
  }

  @NotNull
  public StudyState findStateById(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = studyStateRepository.findOne(id);
    if(studyState == null) throw NoSuchStudyException.withId(id);
    return studyState;
  }

  @NotNull
  @Cacheable(value = "studies-draft", key = "#id")
  public Study findDraftStudy(@NotNull String id) throws NoSuchStudyException {
    // ensure study exists
    findStateById(id);
    return gitService.readHead(id, Study.class);
  }

  @NotNull
  @Cacheable(value = "studies-published", key = "#id")
  public Study findPublishedStudy(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = findStateById(id);
    return gitService.readFromTag(id, studyState.getPublishedTag(), Study.class);
  }

  public boolean isPublished(@NotNull String id) throws NoSuchStudyException {
    return findStateById(id).isPublished();
  }

  public List<StudyState> findAllStates() {
    return studyStateRepository.findAll();
  }

  public List<Study> findAllDraftStudies() {
    return studyStateRepository.findAll().stream()
        .map(studyState -> gitService.readHead(studyState.getId(), Study.class)).collect(Collectors.toList());
  }

  public List<StudyState> findPublishedStates() {
    return studyStateRepository.findByPublishedTagNotNull();
  }

  /**
   * Publish current revision (HEAD)
   *
   * @param id
   * @return
   * @throws NoSuchStudyException
   */
  @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id")
  public StudyState publish(@NotNull String id) throws NoSuchStudyException {
    StudyState studyState = findStateById(id);
    studyState.setRevisionStatus(DRAFT);
    studyState.setPublishedTag(gitService.tag(id));
    studyState.resetRevisionsAhead();
    studyStateRepository.save(studyState);
    eventBus.post(new StudyPublishedEvent(findPublishedStudy(id)));
    return studyState;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
    List<Study> publishedStudies = findPublishedStates().stream()
        .map(studyState -> gitService.readFromTag(studyState.getId(), studyState.getPublishedTag(), Study.class))
        .collect(Collectors.toList());

    eventBus.post(new IndexStudiesEvent(publishedStudies, findAllDraftStudies()));
  }

//  @CacheEvict(value = { "studies-draft", "studies-published" }, key = "#id")
//  public void delete(@NotNull String id) {
//    studyRepository.delete(id);
//  }

  //
  // Private methods
  //

  @Nullable
  private String generateId(@NotNull Study study) {
    ensureAcronym(study);
    return getNextId(study.getAcronym());
  }

  @Nullable
  private String getNextId(LocalizedString suggested) {
    if(suggested == null) return null;
    String prefix = suggested.asString().toLowerCase();
    if(Strings.isNullOrEmpty(prefix)) return null;
    String next = prefix;
    try {
      findStateById(next);
      for(int i = 1; i <= 1000; i++) {
        next = prefix + "-" + i;
        findStateById(next);
      }
      return null;
    } catch(NoSuchStudyException e) {
      return next;
    }
  }

  private void ensureAcronym(@NotNull Study study) {
    if(study.getAcronym() == null || study.getAcronym().isEmpty()) {
      study.setAcronym(study.getName().asAcronym());
    }
  }
}
