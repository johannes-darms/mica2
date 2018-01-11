/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.search.aggregations;

import org.obiba.opal.core.domain.taxonomy.Taxonomy;
import org.springframework.stereotype.Component;

@Component
public class DatasetTaxonomyMetaDataProvider extends ConfigurationTaxonomyMetaDataProvider {

  @Override
  protected Taxonomy getTaxonomy() {
    return taxonomyService.getDatasetTaxonomy();
  }
}
