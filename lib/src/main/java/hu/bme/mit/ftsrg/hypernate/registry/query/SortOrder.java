/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

public enum SortOrder {
  ASC("asc"),
  DESC("desc");

  private final String couchValue;

  SortOrder(final String couchValue) {
    this.couchValue = couchValue;
  }

  public String toCouchValue() {
    return couchValue;
  }
}
