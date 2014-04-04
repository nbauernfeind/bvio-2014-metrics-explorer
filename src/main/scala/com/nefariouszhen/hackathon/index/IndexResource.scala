package com.nefariouszhen.hackathon.index

import javax.ws.rs.{Produces, QueryParam, GET, Path}
import javax.ws.rs.core.MediaType

case class CountResult(word: String, numMatching: Int)

@Path("/index")
@Produces(Array(MediaType.APPLICATION_JSON))
class IndexResource {
  @GET
  def queryIndex(@QueryParam("tag") completed: List[String], @QueryParam("q") prefix: String): Iterable[CountResult] = {
    List()
  }
}
