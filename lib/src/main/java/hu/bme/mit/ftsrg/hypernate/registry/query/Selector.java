/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.util.*;

/**
 * Internal class that accumulates query predicates and produces a CouchDB selector JSON string. Not
 * part of the public API.
 */
class Selector {

  private final Map<String, Map<String, Object>> conditions = new LinkedHashMap<>();
  private final List<Map<String, String>> sortFields = new ArrayList<>();
  private Integer limit;
  private Integer skip;

  void addCondition(final String field, final String operator, final Object value) {
    conditions.computeIfAbsent(field, k -> new LinkedHashMap<>()).put(operator, value);
  }

  void addSort(final String field, final SortOrder order) {
    sortFields.add(Collections.singletonMap(field, order.toCouchValue()));
  }

  void setLimit(final int limit) {
    this.limit = limit;
  }

  void setSkip(final int skip) {
    this.skip = skip;
  }

  String toJSON() {
    final Map<String, Object> query = new LinkedHashMap<>();

    // Build selector
    final Map<String, Object> selector = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, Object>> entry : conditions.entrySet()) {
      String field = entry.getKey();
      Map<String, Object> operators = entry.getValue();

      if (operators.size() == 1 && operators.containsKey("$eq")) {
        // Shorthand: {"field": value} instead of {"field": {"$eq": value}}
        selector.put(field, operators.get("$eq"));
      } else {
        selector.put(field, operators);
      }
    }
    query.put("selector", selector);

    if (!sortFields.isEmpty()) {
      query.put("sort", sortFields);
    }

    if (limit != null) {
      query.put("limit", limit);
    }
    if (skip != null) {
      query.put("skip", skip);
    }

    return JSON.serialize(query);
  }
}
