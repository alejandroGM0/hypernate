/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import hu.bme.mit.ftsrg.hypernate.annotations.AttributeInfo;
import hu.bme.mit.ftsrg.hypernate.annotations.PrimaryKey;
import hu.bme.mit.ftsrg.hypernate.registry.Registry;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class RichQueryBuilderTest {

  @PrimaryKey({@AttributeInfo(name = "id")})
  private record Asset(String id, String color, int size, String owner, double value) {}

  @Mock private ChaincodeStub stub;

  private Registry registry;

  @BeforeEach
  void setup() {
    registry = new Registry(stub);
  }

  @Nested
  class selector_building {

    @Test
    void simple_equality_produces_shorthand_selector() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("color").is("blue"),
          "{\"selector\":{\"color\":\"blue\"}}");
    }

    @Test
    void greater_than_produces_gt_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("size").greaterThan(10),
          "{\"selector\":{\"size\":{\"$gt\":10}}}");
    }

    @Test
    void less_than_produces_lt_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("size").lessThan(100),
          "{\"selector\":{\"size\":{\"$lt\":100}}}");
    }

    @Test
    void greater_or_equal_produces_gte_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("size").greaterOrEqual(5),
          "{\"selector\":{\"size\":{\"$gte\":5}}}");
    }

    @Test
    void less_or_equal_produces_lte_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("value").lessOrEqual(99.9),
          "{\"selector\":{\"value\":{\"$lte\":99.9}}}");
    }

    @Test
    void is_not_produces_ne_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("color").isNot("red"),
          "{\"selector\":{\"color\":{\"$ne\":\"red\"}}}");
    }

    @Test
    void in_produces_in_operator_with_array() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("owner").in("Alice", "Bob"),
          "{\"selector\":{\"owner\":{\"$in\":[\"Alice\",\"Bob\"]}}}");
    }

    @Test
    void not_in_produces_nin_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("owner").notIn("Charlie"),
          "{\"selector\":{\"owner\":{\"$nin\":[\"Charlie\"]}}}");
    }

    @Test
    void regex_produces_regex_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("color").regex("^bl.*"),
          "{\"selector\":{\"color\":{\"$regex\":\"^bl.*\"}}}");
    }

    @Test
    void exists_produces_exists_operator() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("owner").exists(true),
          "{\"selector\":{\"owner\":{\"$exists\":true}}}");
    }

    @Test
    void multiple_conditions_on_different_fields() {
      assertQueryEquals(
          registry
              .richQuery(Asset.class)
              .where("color")
              .is("blue")
              .and("size")
              .greaterThan(10)
              .and("owner")
              .in("Alice", "Bob"),
          "{\"selector\":{\"color\":\"blue\","
              + "\"size\":{\"$gt\":10},"
              + "\"owner\":{\"$in\":[\"Alice\",\"Bob\"]}}}");
    }

    @Test
    void multiple_conditions_on_same_field_are_combined() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("size").greaterThan(10).and("size").lessThan(100),
          "{\"selector\":{\"size\":{\"$gt\":10,\"$lt\":100}}}");
    }

    @Test
    void sort_by_produces_sort_clause() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("color").is("blue").sortBy("value", SortOrder.DESC),
          "{\"selector\":{\"color\":\"blue\"},\"sort\":[{\"value\":\"desc\"}]}");
    }

    @Test
    void multiple_sort_clauses() {
      assertQueryEquals(
          registry
              .richQuery(Asset.class)
              .where("color")
              .is("blue")
              .sortBy("value", SortOrder.DESC)
              .sortBy("size", SortOrder.ASC),
          "{\"selector\":{\"color\":\"blue\"},"
              + "\"sort\":[{\"value\":\"desc\"},{\"size\":\"asc\"}]}");
    }

    @Test
    void limit_produces_limit_clause() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("color").is("blue").limit(50),
          "{\"selector\":{\"color\":\"blue\"},\"limit\":50}");
    }

    @Test
    void skip_produces_skip_clause() {
      assertQueryEquals(
          registry.richQuery(Asset.class).where("color").is("blue").skip(20),
          "{\"selector\":{\"color\":\"blue\"},\"skip\":20}");
    }

    @Test
    void full_query_with_all_clauses() {
      assertQueryEquals(
          registry
              .richQuery(Asset.class)
              .where("color")
              .is("blue")
              .and("size")
              .greaterThan(10)
              .and("owner")
              .in("Alice", "Bob")
              .sortBy("value", SortOrder.DESC)
              .limit(50)
              .skip(10),
          "{\"selector\":{\"color\":\"blue\","
              + "\"size\":{\"$gt\":10},"
              + "\"owner\":{\"$in\":[\"Alice\",\"Bob\"]}},"
              + "\"sort\":[{\"value\":\"desc\"}],"
              + "\"limit\":50,"
              + "\"skip\":10}");
    }

    private void assertQueryEquals(RichQueryBuilder<?> builder, String expectedJson) {
      given(stub.getQueryResult(anyString())).willReturn(emptyQueryResults());
      builder.execute();
      ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
      then(stub).should().getQueryResult(captor.capture());
      assertThat(captor.getValue()).isEqualTo(expectedJson);
    }
  }

  @Nested
  class validation {

    @Test
    void limit_must_be_positive() {
      RichQueryBuilder<Asset> builder = registry.richQuery(Asset.class);

      assertThatThrownBy(() -> builder.limit(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Limit must be a positive integer");

      assertThatThrownBy(() -> builder.limit(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skip_must_be_non_negative() {
      RichQueryBuilder<Asset> builder = registry.richQuery(Asset.class);

      assertThatThrownBy(() -> builder.skip(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Skip must be a non-negative integer");
    }
  }

  @Nested
  class execution {

    @Test
    void execute_returns_empty_list_when_no_results() {
      given(stub.getQueryResult(anyString())).willReturn(emptyQueryResults());

      List<Asset> results = registry.richQuery(Asset.class).where("color").is("blue").execute();

      assertThat(results).isEmpty();
    }

    @Test
    void execute_deserializes_results_into_entities() {
      Asset asset1 = new Asset("asset1", "blue", 15, "Alice", 100.0);
      Asset asset2 = new Asset("asset2", "blue", 25, "Bob", 200.0);

      given(stub.getQueryResult(anyString())).willReturn(queryResultsOf(asset1, asset2));

      List<Asset> results = registry.richQuery(Asset.class).where("color").is("blue").execute();

      assertThat(results).hasSize(2);
      assertThat(results.get(0).id()).isEqualTo("asset1");
      assertThat(results.get(0).color()).isEqualTo("blue");
      assertThat(results.get(0).size()).isEqualTo(15);
      assertThat(results.get(0).owner()).isEqualTo("Alice");
      assertThat(results.get(1).id()).isEqualTo("asset2");
      assertThat(results.get(1).owner()).isEqualTo("Bob");
    }

    @Test
    void execute_with_complex_query() {
      Asset asset = new Asset("asset1", "blue", 15, "Alice", 100.0);

      given(stub.getQueryResult(anyString())).willReturn(queryResultsOf(asset));

      List<Asset> results =
          registry
              .richQuery(Asset.class)
              .where("color")
              .is("blue")
              .and("size")
              .greaterThan(10)
              .and("owner")
              .in("Alice", "Bob")
              .sortBy("value", SortOrder.DESC)
              .limit(50)
              .execute();

      assertThat(results).hasSize(1);
      assertThat(results.get(0).id()).isEqualTo("asset1");

      ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
      then(stub).should().getQueryResult(queryCaptor.capture());
      assertThat(queryCaptor.getValue()).contains("\"selector\"");
      assertThat(queryCaptor.getValue()).contains("\"color\":\"blue\"");
      assertThat(queryCaptor.getValue()).contains("\"$gt\":10");
      assertThat(queryCaptor.getValue()).contains("\"limit\":50");
    }

    @Test
    void no_stub_interaction_before_execute() {
      registry
          .richQuery(Asset.class)
          .where("color")
          .is("blue")
          .and("size")
          .greaterThan(10)
          .sortBy("value", SortOrder.DESC)
          .limit(50);

      verifyNoInteractions(stub);
    }
  }

  // --- Helpers ---

  private QueryResultsIterator<KeyValue> emptyQueryResults() {
    return new QueryResultsIterator<>() {
      @Override
      public void close() {}

      @Override
      public @Nonnull Iterator<KeyValue> iterator() {
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return false;
          }

          @Override
          public KeyValue next() {
            throw new java.util.NoSuchElementException();
          }
        };
      }
    };
  }

  @SafeVarargs
  private final QueryResultsIterator<KeyValue> queryResultsOf(Asset... assets) {
    List<KeyValue> kvList = new java.util.ArrayList<>();
    for (Asset asset : assets) {
      String json = JSON.serialize(asset);
      byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
      kvList.add(
          new KeyValue() {
            @Override
            public String getKey() {
              return asset.id();
            }

            @Override
            public byte[] getValue() {
              return bytes;
            }

            @Override
            public String getStringValue() {
              return json;
            }
          });
    }

    return new QueryResultsIterator<>() {
      @Override
      public void close() {}

      @Override
      public @Nonnull Iterator<KeyValue> iterator() {
        return kvList.iterator();
      }
    };
  }
}
