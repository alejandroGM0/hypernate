/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fluent builder for CouchDB rich queries. Produces a CouchDB selector JSON and executes it via
 * {@code stub.getQueryResult()}.
 *
 * <p>Important: Rich queries read directly from CouchDB and will not see uncommitted writes
 * buffered by the middleware cache within the same transaction.
 *
 * <p>Usage example:
 *
 * <p>{@code List results = registry.richQuery(Asset.class) .where("color").is("blue")
 * .and("size").greaterThan(10) .and("owner").in("Alice", "Bob") .sortBy("value", SortOrder.DESC)
 * .limit(50) .execute(); }
 *
 * @param the entity type being queried
 */
@Loggable(Loggable.DEBUG)
public class RichQueryBuilder<T> {

  private static final Logger logger = LoggerFactory.getLogger(RichQueryBuilder.class);

  private final ChaincodeStub stub;
  private final Class<T> clazz;
  private final Selector selector;

  public RichQueryBuilder(final ChaincodeStub stub, final Class<T> clazz) {
    this.stub = stub;
    this.clazz = clazz;
    this.selector = new Selector();
  }

  /**
   * Start a predicate on a field. This is the first predicate in the query.
   *
   * @param fieldName the name of the field to filter on
   * @return a {@link FieldPredicate} to specify the comparison
   */
  public FieldPredicate<T> where(final String fieldName) {
    return new FieldPredicate<>(this, fieldName);
  }

  /**
   * Add an additional predicate (AND semantics).
   *
   * @param fieldName the name of the field to filter on
   * @return a {@link FieldPredicate} to specify the comparison
   */
  public FieldPredicate<T> and(final String fieldName) {
    return new FieldPredicate<>(this, fieldName);
  }

  /**
   * Add a sort clause.
   *
   * @param fieldName the field to sort by
   * @param order ascending or descending
   * @return this builder for chaining
   */
  public RichQueryBuilder<T> sortBy(final String fieldName, final SortOrder order) {
    selector.addSort(fieldName, order);
    return this;
  }

  /**
   * Limit the number of results returned.
   *
   * @param limit maximum number of results
   * @return this builder for chaining
   */
  public RichQueryBuilder<T> limit(final int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("Limit must be a positive integer");
    }
    selector.setLimit(limit);
    return this;
  }

  /**
   * Skip a number of results (for offset-based pagination).
   *
   * @param offset number of results to skip
   * @return this builder for chaining
   */
  public RichQueryBuilder<T> skip(final int offset) {
    if (offset < 0) {
      throw new IllegalArgumentException("Skip must be a non-negative integer");
    }
    selector.setSkip(offset);
    return this;
  }

  /**
   * Execute the query against CouchDB and return deserialized results.
   *
   * @return list of matching entities (empty if no matches, never null)
   * @throws QueryException if the query fails to execute
   */
  public List<T> execute() {
    final String queryJson = selector.toJSON();
    logger.info("Executing rich query for {}: {}", clazz.getName(), queryJson);

    try {
      final List<T> results = new ArrayList<>();
      for (KeyValue kv : stub.getQueryResult(queryJson)) {
        final String json = new String(kv.getValue(), StandardCharsets.UTF_8);
        logger.debug("Rich query result at key {}: {}", kv.getKey(), json);
        results.add(JSON.deserialize(json, clazz));
      }
      logger.info("Rich query returned {} results", results.size());
      return results;
    } catch (Exception e) {
      throw new QueryException("Rich query execution failed: " + e.getMessage(), e);
    }
  }

  /**
   * Internal method called by {@link FieldPredicate} to register a condition.
   *
   * @param field the field name
   * @param operator the CouchDB operator (e.g. "$eq", "$gt")
   * @param value the value to compare against
   * @return this builder for chaining
   */
  RichQueryBuilder<T> addCondition(final String field, final String operator, final Object value) {
    selector.addCondition(field, operator, value);
    return this;
  }
}
