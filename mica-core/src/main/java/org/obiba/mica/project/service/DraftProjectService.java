/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.project.service;

import org.obiba.mica.core.service.DraftDocumentService;
import org.obiba.mica.project.domain.Project;

public interface DraftProjectService extends DraftDocumentService<Project> {

  ProjectService getProjectService();

  default Project findById(String id) {
    return getProjectService().findById(id);
  }

}
