package updown.app.model

/* I'm not totally convinced this is the ideal solution here.
What I'm going for is that evaluations should perform all their work and _return_ their results
instead of printing them. This yields two benefits: 1 the functionality is testable,
and 2 the functionality becomes available to other code, which can perform the evaluation, receive
the result and do something with it, which is obviously not possible when the return value is Unit.

That said, the same could be achieved by returning a tuple, but I wanted to provide more documentation
about the meaning of the tuple values than the final line of the computeEvaluation function.
*/
abstract class Result

case class EvaluationResult(correct: Double, total: Double, skipped: Double, error: Double, message: String) extends Result