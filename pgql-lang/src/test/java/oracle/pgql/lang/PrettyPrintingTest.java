/*
 * Copyright (C) 2013 - 2018 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgql.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import oracle.pgql.lang.ir.GraphQuery;
import oracle.pgql.lang.ir.QueryExpression.Constant.ConstString;
import oracle.pgql.lang.ir.QueryExpression.FunctionCall;
import oracle.pgql.lang.ir.QueryExpression.PropertyAccess;;

public class PrettyPrintingTest extends AbstractPgqlTest {

  @Test
  public void testBasicGraphPattern1() throws Exception {
    String query = "SELECT n.name FROM MATCH( (n) -> (m) ) WHERE m.prop1 = 'abc' AND n.prop2 = m.prop2";
    checkRoundTrip(query);
  }

  @Test
  public void testBasicGraphPattern1Reverse() throws Exception {
    String query = "SELECT n.name FROM MATCH( (n) <- (m) ) WHERE m.prop1 = 'abc' AND n.prop2 = m.prop2";
    checkRoundTrip(query);
  }

  @Test
  public void testBasicGraphPattern2() throws Exception {
    String query = "SELECT n.name FROM MATCH( (n) -[e]-> () ) WHERE e.weight = 10 OR e.weight < n.weight";
    checkRoundTrip(query);
  }

  @Test
  public void testBasicGraphPattern2Reverse() throws Exception {
    String query = "SELECT n.name FROM MATCH( (n) <-[e]- () ) WHERE e.weight = 10 OR e.weight < n.weight";
    checkRoundTrip(query);
  }

  @Test
  public void testBasicGraphPattern3() throws Exception {
    String query = "SELECT n.name FROM MATCH( (n) -> () ) WHERE n.prop1 = 10";
    checkRoundTrip(query);
  }

  @Test
  public void testPathQuery1() throws Exception {
    String query = "SELECT n.name, m.name FROM MATCH( (n) -/:likes*/-> (m) )";
    checkRoundTrip(query);
  }

  @Test
  public void testPathQuery1Reverse() throws Exception {
    String query = "SELECT n.name, m.name FROM MATCH( (n) <-/:likes*/- (m) )";
    checkRoundTrip(query);
  }

  @Test
  public void testPathQuery2() throws Exception {
    String query = "PATH knows AS (n:Person) -[e:likes|dislikes]-> (m:Person) SELECT n.name, m.name FROM MATCH( (n) -/:knows*/-> (m) )";
    checkRoundTrip(query);
  }

  @Test
  public void testPredicatesOnAnonymousVariables() throws Exception {
    String query = "SELECT m.name FROM MATCH( (:a|b) -> (m) )";
    checkRoundTrip(query);
  }

  @Test
  public void testQueryWithOrderBy() throws Exception {
    String query = "SELECT m.name, m.age FROM MATCH( (m)->(n) ) ORDER BY m.age";
    checkRoundTrip(query);
  }

  @Test
  public void testQueryWithOrderByLimit() throws Exception {
    String query = "SELECT m.name, m.age FROM MATCH( (m)->(n) ) ORDER BY m.age LIMIT 10";
    checkRoundTrip(query);
  }

  @Test
  public void testQueryWithOrderByOffsetLimit() throws Exception {
    String query = "SELECT m.name, m.age FROM MATCH( (m)->(n) ) ORDER BY m.age OFFSET 2 LIMIT 1";
    checkRoundTrip(query);
  }

  @Test
  public void testQueryWithFromClause() throws Exception {
    String query = "SELECT m.name, n.age FROM persons MATCH( (m)->(n) )";
    checkRoundTrip(query);
  }

  @Test
  public void testUndirectedEdge() throws Exception {
    String query = "SELECT m.name, m.age FROM MATCH( (m)-(n) )";
    checkRoundTrip(query);
  }

  @Test
  public void testAggregation() throws Exception {
    String query = "SELECT COUNT(*) AS count, AVG(n.age) AS AVG FROM MATCH( (n) )";
    checkRoundTrip(query);
  }

  @Test
  public void testDistinct() throws Exception {
    String query = "SELECT DISTINCT " //
        + "COUNT(DISTINCT n.age) AS count," //
        + "MIN(DISTINCT n.age)," //
        + "MAX(DISTINCT n.age)," //
        + "AVG(DISTINCT n.age)," //
        + "SUM(DISTINCT n.age)" //
        + "FROM MATCH( (n) )";
    checkRoundTrip(query);
  }

  @Test
  public void testDateTime() throws Exception {
    String query = "SELECT " //
        + "DATE '2017-01-01', "//
        + "TIME '20:00:00', " //
        + "TIMESTAMP '2017-01-01 20:00:00', "//
        + "TIME '20:00:00.1234+01:00', "//
        + "TIMESTAMP '2017-01-01 20:00:00.1234-01:00'" //
        + "FROM MATCH( () )";
    checkRoundTrip(query);
  }

  @Test
  public void testStringEscaping() throws Exception {
    String query = "SELECT '\\'\"\\\"\\\\\\t\\n\\r\\b\\f'" //
        + "FROM MATCH( () )";
    checkRoundTrip(query);
  }

  @Test
  public void testExistsQuery() throws Exception {
    String query = "SELECT id(n) FROM MATCH( (n) ) WHERE EXISTS (SELECT 1 FROM MATCH( (n) -[:likes]-> (m) ))";
    checkRoundTrip(query);
  }

  @Test
  public void testExistsInAggregation() throws Exception {
    String query = "SELECT MAX(EXISTS (SELECT * FROM MATCH( (m)->(o) ) WHERE o.age > n.age)) FROM MATCH( (n)->(m) )";
    checkRoundTrip(query);
  }

  @Test
  public void testExistsInOrderBy() throws Exception {
    String query = "SELECT id(n), 3 AS three FROM MATCH( (n) ) ORDER BY EXISTS ( SELECT * FROM MATCH( (m) ) WHERE m.age + three = n.age), id(n)";
    checkRoundTrip(query);
  }

  @Test
  public void testIdentifierEscaping() throws Exception {
    String identifier = "\"\"";
    String escapedIdentifier = "\"\\\"\"\"\"";
    String query = "SELECT n." + escapedIdentifier + " FROM " + escapedIdentifier + " MATCH( (n:" + escapedIdentifier
        + "))";
    GraphQuery graphQuery = pgql.parse(query).getGraphQuery();

    PropertyAccess propertyAccess = (PropertyAccess) graphQuery.getProjection().getElements().get(0).getExp();
    assertEquals(propertyAccess.getPropertyName(), identifier);

    assertEquals(identifier, graphQuery.getInputGraphName());

    FunctionCall funcCall = (FunctionCall) graphQuery.getGraphPattern().getConstraints().iterator().next();
    assertEquals(funcCall.getFunctionName(), "has_label");
    String label = ((ConstString) funcCall.getArgs().get(1)).getValue();
    assertEquals(identifier, label);
  }

  @Test
  public void testHaving() throws Exception {
    String query = "SELECT n.age, COUNT(*) FROM MATCH( (n) ) GROUP BY n.age HAVING COUNT(*) > 100";
    checkRoundTrip(query);
  }

  @Test
  public void testShortest1() throws Exception {
    String query = "SELECT n.prop FROM MATCH( SHORTEST (n) (-[e:lbl]-> WHERE e.prop = 123)* (o) )";
    checkRoundTrip(query);
  }

  @Test
  public void testShortest2() throws Exception {
    String query = "SELECT ARRAY_AGG(e.weight) AS weights FROM MATCH( () -> (n), SHORTEST (n) -[e]->* (m), (m) -> (o) )";
    checkRoundTrip(query);
  }

  private void checkRoundTrip(String query1) throws PgqlException {

    /*
     * First, assert that when parsing a query into a GraphQuery object and then pretty printing that GraphQuery object,
     * we obtain a string that is a valid PGQL query.
     */
    PgqlResult result1 = pgql.parse(query1);
    GraphQuery iR1 = result1.getGraphQuery();
    assertTrue(result1.getErrorMessages(), result1.isQueryValid() && iR1 != null);
    String query2 = iR1.toString();
    PgqlResult result2 = pgql.parse(query2);
    GraphQuery iR2 = result2.getGraphQuery();
    assertTrue(result2.getErrorMessages(), iR2 != null);
    assertTrue(result2.getErrorMessages(), result2.isQueryValid() && iR1 != null);

    /*
     * Since pretty-printed queries are in normal form, we can now round trip endlessly. Here, we assert that when
     * pretty-printing a GraphQuery object that was parsed from a pretty-printed query, we obtain another GraphQuery
     * object that is equal to the first.
     */
    String query3 = iR2.toString();
    GraphQuery iR3 = pgql.parse(query3).getGraphQuery();
    assertEquals(iR2, iR3);
  }
}