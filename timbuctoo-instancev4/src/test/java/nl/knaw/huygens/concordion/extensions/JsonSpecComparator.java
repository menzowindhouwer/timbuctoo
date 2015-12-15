package nl.knaw.huygens.concordion.extensions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONString;
import org.skyscreamer.jsonassert.*;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import java.text.MessageFormat;

public class JsonSpecComparator extends DefaultComparator {
  RegularExpressionValueMatcher<Object> regexMatcher = new RegularExpressionValueMatcher<>();
  ArraySpecMatcher<Object> arrayMatcher = new ArraySpecMatcher<>(this);

  public JsonSpecComparator() {
    super(JSONCompareMode.LENIENT);
  }

  @Override
  public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result) throws JSONException {
    if (expectedValue instanceof JSONArray) {
      try {
        arrayMatcher.equal(prefix, actualValue, expectedValue, result);
      } catch (ValueMatcherException e) {
        result.fail(prefix, e);
      }
    } else if (expectedValue instanceof JSONString || expectedValue instanceof String) {
      String expectationString = expectedValue.toString();
      if (expectationString.startsWith("?")) {
        if (expectationString.equals("?Number") || expectationString.equals("?Boolean")) {
          if (expectationString.equals("?Number") && !(actualValue instanceof Number)) {
            result.fail(prefix, "a number", actualValue);
          } else if (expectationString.equals("?Boolean") && !(actualValue instanceof Boolean)) {
            result.fail(prefix, "a boolean", actualValue);
          }
        } else {
          result.fail(prefix, new ValueMatcherException("Not a valid expectation", "?Number or ?Boolean", expectationString));
        }
      } else {
        try {
          if (!regexMatcher.equal(actualValue.toString(), expectationString)) {
            result.fail(prefix, expectedValue, actualValue);
          }
        } catch (ValueMatcherException e) {
          result.fail(prefix, e);
        }
      }
    } else {
      super.compareValues(prefix, expectedValue, actualValue, result);
    }
  }

  private class ArraySpecMatcher<T> implements LocationAwareValueMatcher<T> {
    private final JSONComparator comparator;

    public ArraySpecMatcher(JSONComparator subComparator) {
      comparator = subComparator;
    }

    @Override
    public boolean equal(T o1, T o2) {
      throw new UnsupportedOperationException("Use the Location Aware equals");
    }

    @Override
    public boolean equal(String prefix, T actual, T expected, JSONCompareResult result) {
      try {
        JSONArray actualArray = (JSONArray) actual;
        JSONArray expectedArray = expected instanceof JSONArray ? (JSONArray) expected : new JSONArray(new Object[]{expected});
        for (int i = 0; i < actualArray.length(); i++) {
          String elementPrefix = MessageFormat.format("{0}[{1}]", prefix, i);
          Object actualElement = actualArray.get(i);
          int successCount = 0;
          for (int j = 0; j < expectedArray.length(); j++) {
            JSONCompareResult tempResult = new JSONCompareResult();
            Object expectedElement = expectedArray.get(j);
            comparator.compareValues(elementPrefix, expectedElement, actualElement, tempResult);
            if (tempResult.passed()) {
              successCount++;
            }
          }
          if (successCount == 0) {
            result.fail(prefix, new ValueMatcherException("Array item matched none of the suggestions", "", actualElement.toString()));
          } else if (successCount > 1) {
            result.fail(prefix, new ValueMatcherException("Array item matched more then one suggestion", "", actualElement.toString()));
          } else {
            result.passed();
          }
        }
        // any failures have already been passed to result, so return true
        return true;
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
  }
}