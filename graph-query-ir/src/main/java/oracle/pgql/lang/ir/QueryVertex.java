/*
 * Copyright (C) 2013 - 2019 Oracle and/or its affiliates. All rights reserved.
 */
package oracle.pgql.lang.ir;

public class QueryVertex extends QueryVariable {

  public QueryVertex(String name, boolean anonymous) {
    super(name, anonymous);
  }

  @Override
  public VariableType getVariableType() {
    return VariableType.VERTEX;
  }

  @Override
  public String toString() {
    if (isAnonymous()) {
      return "()";
    } else {
      return "(" + name + ")";
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QueryVariable that = (QueryVariable) o;

    if (anonymous != that.anonymous) {
      return false;
    }
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  public void accept(QueryExpressionVisitor v) {
    v.visit(this);
  }
}
