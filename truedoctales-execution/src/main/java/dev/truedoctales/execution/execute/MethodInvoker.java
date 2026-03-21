package dev.truedoctales.execution.execute;

import dev.truedoctales.api.model.execution.InputType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/// Handles method invocation with automatic parameter conversion.
///
/// Converts table data and inplaceVariables to method parameters, supporting:
/// - Simple parameters (String, Long, Integer, Boolean, LocalDate, enums)
/// - Data table rows as individual parameters
/// - Data table rows as typed collections (List<Record>, List<Map>)
public class MethodInvoker {

  private static final Logger LOGGER = Logger.getLogger(MethodInvoker.class.getName());

  /// Invokes a method with data from table rows and extracted inplaceVariables.
  ///
  /// @param instance  the object instance
  /// @param method    the method to invoke
  /// @param inputType  sequence or batch mode
  /// @param maps      the table data rows
  /// @param variables extracted inplaceVariables from pattern matching
  /// @return the method result
  /// @throws Exception if invocation fails
  public Object invoke(
      Object instance,
      Method method,
      InputType inputType,
      List<Map<String, String>> maps,
      Map<String, String> variables)
      throws Exception {
    return switch (inputType) {
      case NONE -> invokeWithoutParameters(instance, method);
      case SEQUENCE -> {
        if (maps.isEmpty()) {
          yield invokeWithVariables(instance, method, variables);
        } else {
          yield invokeWithEachDataRow(instance, method, maps, variables);
        }
      }
      case BATCH -> invokeWithDataCollection(instance, method, maps, variables);
    };
  }

  private Object invokeWithEachDataRow(
      Object instance, Method method, List<Map<String, String>> maps, Map<String, String> variables)
      throws Exception {
    var results = new ArrayList<>();
    for (Map<String, String> map : maps) {
      // Merge inplaceVariables with each row
      Map<String, String> mergedData = new HashMap<>(variables);
      mergedData.putAll(map);
      results.add(invokeWithDataRow(instance, method, mergedData));
    }
    // If there are no table rows, but we have inplaceVariables, invoke once with just the
    // inplaceVariables
    if (maps.isEmpty() && !variables.isEmpty()) {
      results.add(invokeWithDataRow(instance, method, variables));
    }
    return results;
  }

  Object invokeWithoutParameters(Object instance, Method method) throws Exception {
    method.setAccessible(true);
    return method.invoke(instance);
  }

  Object invokeWithVariables(Object instance, Method method, Map<String, String> variables)
      throws Exception {
    method.setAccessible(true);
    Object[] args = prepareArgumentsFromVariables(method, variables);
    return method.invoke(instance, args);
  }

  Object invokeWithDataRow(Object instance, Method method, Map<String, String> methodCall)
      throws Exception {
    method.setAccessible(true);
    Object[] args = prepareArgumentsFromDataRow(methodCall, method.getParameters());
    return method.invoke(instance, args);
  }

  Object invokeWithDataCollection(
      Object instance,
      Method method,
      List<Map<String, String>> methodCalls,
      Map<String, String> variables)
      throws Exception {
    method.setAccessible(true);
    Object[] args = prepareArgumentsFromDataCollection(method, methodCalls, variables);
    return method.invoke(instance, args);
  }

  private Object[] prepareArgumentsFromVariables(Method method, Map<String, String> variables) {
    Parameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      Parameter param = parameters[i];
      String value = variables.get(param.getName());
      args[i] = convertValue(value, param.getType(), param.getName());
    }

    return args;
  }

  private Object[] prepareArgumentsFromDataRow(
      Map<String, String> methodCall, Parameter[] parameters) {
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      Parameter param = parameters[i];
      String value = findParameterValue(param, methodCall);
      args[i] = convertValue(value, param.getType(), param.getName());
    }

    return args;
  }

  private Object[] prepareArgumentsFromDataCollection(
      Method method, List<Map<String, String>> methodCalls, Map<String, String> variables) {
    Parameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];

    validateSingleCollectionParameter(method, parameters);

    boolean collectionAssigned = assignArguments(parameters, args, methodCalls, variables);

    if (!collectionAssigned) {
      throwNoCollectionParameterError(method);
    }

    return args;
  }

  private void validateSingleCollectionParameter(Method method, Parameter[] parameters) {
    long collectionParamCount = countCollectionParameters(parameters);

    if (collectionParamCount > 1) {
      throwMultipleCollectionParametersError(method);
    }
  }

  private long countCollectionParameters(Parameter[] parameters) {
    long count = 0;
    for (Parameter param : parameters) {
      if (isCollectionType(param.getType())) {
        count++;
      }
    }
    return count;
  }

  private boolean isCollectionType(Class<?> type) {
    return List.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
  }

  private boolean assignArguments(
      Parameter[] parameters,
      Object[] args,
      List<Map<String, String>> dataList,
      Map<String, String> variables) {
    boolean collectionAssigned = false;
    for (int i = 0; i < parameters.length; i++) {
      Parameter param = parameters[i];
      if (isCollectionType(param.getType())) {
        args[i] = convertDataListToTypedCollection(param, dataList);
        collectionAssigned = true;
      } else {
        args[i] = convertValue(variables.get(param.getName()), param.getType(), param.getName());
      }
    }
    return collectionAssigned;
  }

  private void throwMultipleCollectionParametersError(Method method) {
    String error =
        "Method "
            + method.getName()
            + " has multiple collection parameters. "
            + "Only one collection parameter is supported per method.";
    LOGGER.severe(error);
    throw new IllegalArgumentException(error);
  }

  private void throwNoCollectionParameterError(Method method) {
    String error =
        "Method "
            + method.getName()
            + " was expected to have a collection parameter but none found";
    LOGGER.severe(error);
    throw new IllegalArgumentException(error);
  }

  private Object convertDataListToTypedCollection(
      Parameter param, List<Map<String, String>> dataList) {
    Type genericType = param.getParameterizedType();

    // If not a parameterized type, return raw maps
    if (!(genericType instanceof ParameterizedType parameterizedType)) {
      LOGGER.fine("Collection parameter has no generic type, returning raw maps");
      return dataList;
    }

    Type[] typeArguments = parameterizedType.getActualTypeArguments();

    // If no type arguments, return raw maps
    if (typeArguments.length == 0) {
      LOGGER.fine("Collection parameter has no type arguments, returning raw maps");
      return dataList;
    }

    Type elementType = typeArguments[0];

    // If element type is Map<String, String>, return raw maps
    if (elementType instanceof ParameterizedType elementParamType) {
      if (Map.class.isAssignableFrom((Class<?>) elementParamType.getRawType())) {
        LOGGER.fine("Collection element type is Map, returning raw maps");
        return dataList;
      }
    }

    // If element type is a class (e.g., Person, Country), convert maps to objects
    if (elementType instanceof Class<?> elementClass) {
      LOGGER.fine("Converting data list to List<" + elementClass.getSimpleName() + ">");
      return convertMapsToObjects(dataList, elementClass);
    }

    // Default: return raw maps
    LOGGER.fine("Unable to determine element type, returning raw maps");
    return dataList;
  }

  private List<?> convertMapsToObjects(List<Map<String, String>> dataList, Class<?> targetClass) {
    List<Object> result = new ArrayList<>();

    // Find the canonical constructor (for records) or constructor with most parameters
    Constructor<?> constructor = findBestConstructor(targetClass);
    if (constructor == null) {
      String error = "No suitable constructor found for type " + targetClass.getSimpleName();
      LOGGER.severe(error);
      throw new IllegalArgumentException(error);
    }

    // Convert each map to an object
    for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
      Map<String, String> dataRow = dataList.get(rowIndex);
      try {
        Object obj = createObjectFromMap(constructor, dataRow);
        result.add(obj);
      } catch (Exception e) {
        String error =
            String.format(
                "Failed to create %s instance from data row %d: %s",
                targetClass.getSimpleName(), rowIndex, e.getMessage());
        LOGGER.log(Level.SEVERE, error, e);
        throw new IllegalArgumentException(error, e);
      }
    }

    return result;
  }

  private Constructor<?> findBestConstructor(Class<?> targetClass) {
    Constructor<?>[] constructors = targetClass.getDeclaredConstructors();

    if (constructors.length == 0) {
      return null;
    }

    // For records, find the canonical constructor (the one with all fields)
    // For other classes, find the constructor with the most parameters
    Constructor<?> bestConstructor = constructors[0];
    for (Constructor<?> constructor : constructors) {
      if (constructor.getParameterCount() > bestConstructor.getParameterCount()) {
        bestConstructor = constructor;
      }
    }

    return bestConstructor;
  }

  private Object createObjectFromMap(Constructor<?> constructor, Map<String, String> dataRow)
      throws Exception {
    Parameter[] parameters = constructor.getParameters();
    Object[] args = prepareArgumentsFromDataRow(dataRow, parameters);
    constructor.setAccessible(true);
    return constructor.newInstance(args);
  }

  private String findParameterValue(Parameter param, Map<String, String> dataRow) {
    String paramName = param.getName();
    String value = dataRow.get(paramName);

    if (value != null) {
      return value;
    }

    // Try case-insensitive match
    for (Map.Entry<String, String> entry : dataRow.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(paramName)
          || entry.getKey().replace("_", "").equalsIgnoreCase(paramName)) {
        return entry.getValue();
      }
    }

    return null;
  }

  private Object convertValue(String value, Class<?> targetType, String paramName) {
    if (value == null) {
      handleNullValue(targetType, paramName);
      return null;
    }

    try {
      return convertNonNullValue(value, targetType);
    } catch (Exception e) {
      String error =
          String.format(
              "Failed to convert parameter '%s' (value: '%s') to type %s",
              paramName, value, targetType.getSimpleName());
      LOGGER.log(Level.SEVERE, error, e);
      throw new IllegalArgumentException(error, e);
    }
  }

  private void handleNullValue(Class<?> targetType, String paramName) {
    if (targetType.isPrimitive()) {
      String error = "Cannot convert null to primitive type: " + targetType;
      LOGGER.severe(error);
      throw new IllegalArgumentException(error);
    }
    LOGGER.fine("Parameter '" + paramName + "' is null");
  }

  private Object convertNonNullValue(String value, Class<?> targetType) {
    if (targetType == String.class) {
      return value;
    }
    if (targetType == Long.class || targetType == long.class) {
      return Long.parseLong(value);
    }
    if (targetType == Integer.class || targetType == int.class) {
      return Integer.parseInt(value);
    }
    if (targetType == Float.class || targetType == float.class) {
      return Float.parseFloat(value);
    }
    if (targetType == Double.class || targetType == double.class) {
      return Double.parseDouble(value);
    }
    if (targetType == LocalDate.class) {
      return LocalDate.parse(value);
    }
    if (targetType == Boolean.class || targetType == boolean.class) {
      return Boolean.parseBoolean(value);
    }
    if (targetType.isEnum()) {
      return convertToEnum(value, targetType);
    }

    throw new IllegalArgumentException("Unsupported parameter type: " + targetType);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object convertToEnum(String value, Class<?> enumType) {
    return Enum.valueOf((Class<Enum>) enumType, value);
  }
}
