package dev.truedoctales.api.execute;

import dev.truedoctales.api.model.execution.StepExecution;
import dev.truedoctales.api.model.plot.PlotBinding;
import java.util.Set;

/// Interface for registering plot instances and invoking bound binding methods.
///
/// The plot registry manages the bindings between plot names/binding patterns and their
/// implementation methods.
public interface PlotRegistry {

  /// Returns all registered plot bindings.
  ///
  /// @return set of plot bindings
  Set<PlotBinding> getBindings();

  /// Invokes the method bound to the given binding execution.
  ///
  /// @param stepExecution the binding execution to invoke
  /// @return the result of the method invocation
  /// @throws Exception if method invocation fails
  Object invoke(StepExecution stepExecution) throws Exception;
}
