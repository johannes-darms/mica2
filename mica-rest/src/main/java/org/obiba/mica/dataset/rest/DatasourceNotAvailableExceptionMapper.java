/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.dataset.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.obiba.mica.dataset.service.DatasourceNotAvailableException;

@Provider
public class DatasourceNotAvailableExceptionMapper implements ExceptionMapper<DatasourceNotAvailableException> {
  @Override
  public Response toResponse(DatasourceNotAvailableException e) {
    return Response.status(Status.SERVICE_UNAVAILABLE).entity("Verify the datasource is available.").build();
  }
}
