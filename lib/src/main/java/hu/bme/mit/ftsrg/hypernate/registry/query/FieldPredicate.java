/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import java.util.Arrays;

/**
 * Intermediate object in the fluent query chain. Returned by {@link RichQueryBuilder#where} and
 * {@link RichQueryBuilder#and} to capture a field name, then resolved by a comparison method back
 * to the builder.
 *
 * @param the entity type being queried
 */
public class FieldPredicate<T> {

  private final RichQueryBuilder<T> builder;
  private final String fieldName;

  FieldPredicate(final RichQueryBuilder<T> builder, final String fieldName) {
    this.builder = builder;
    this.fieldName = fieldName;
  }

  public RichQueryBuilder<T> is(final Object value) {
    return builder.addCondition(fieldName, "$eq", value);
  }

  public RichQueryBuilder<T> isNot(final Object value) {
    return builder.addCondition(fieldName, "$ne", value);
  }

  public RichQueryBuilder<T> greaterThan(final Object value) {
    return builder.addCondition(fieldName, "$gt", value);
  }

  public RichQueryBuilder<T> lessThan(final Object value) {
    return builder.addCondition(fieldName, "$lt", value);
  }

  public RichQueryBuilder<T> greaterOrEqual(final Object value) {
    return builder.addCondition(fieldName, "$gte", value);
  }

  public RichQueryBuilder<T> lessOrEqual(final Object value) {
    return builder.addCondition(fieldName, "$lte", value);
  }

  public RichQueryBuilder<T> in(final Object... values) {
    return builder.addCondition(fieldName, "$in", Arrays.asList(values));
  }

  public RichQueryBuilder<T> notIn(final Object... values) {
    return builder.addCondition(fieldName, "$nin", Arrays.asList(values));
  }

  public RichQueryBuilder<T> exists(final boolean exists) {
    return builder.addCondition(fieldName, "$exists", exists);
  }

  public RichQueryBuilder<T> regex(final String pattern) {
    return builder.addCondition(fieldName, "$regex", pattern);
  }
}
