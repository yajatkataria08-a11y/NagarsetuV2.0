/** validateBody(schema): 400 with a field-level issue list, otherwise next(). Does not mutate req.body. */
function validateBody(schema) {
  return (req, res, next) => {
    const r = schema.safeParse(req.body ?? {});
    if (r.success) return next();
    return res.status(400).json({
      error: 'Invalid request body',
      issues: r.error.issues.slice(0, 10).map(i => ({ path: i.path.join('.'), message: i.message })),
    });
  };
}
module.exports = { validateBody };
