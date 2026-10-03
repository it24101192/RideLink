const TRANSITIONS = Object.freeze({
  REQUESTED: Object.freeze(['ASSIGNED', 'CANCELLED']),
  ASSIGNED: Object.freeze(['ACCEPTED', 'CANCELLED']),
  ACCEPTED: Object.freeze(['IN_PROGRESS', 'CANCELLED']),
  IN_PROGRESS: Object.freeze(['COMPLETED']),
  COMPLETED: Object.freeze([]),
  CANCELLED: Object.freeze([]),
});

class InvalidTransitionError extends Error {
  constructor(from, to) {
    super(`Invalid Status Transition: ${from} -> ${to}`);
    this.name = 'InvalidTransitionError';
    this.code = 'INVALID_STATUS_TRANSITION';
    this.statusCode = 400;
    this.from = from;
    this.to = to;
  }
}

function validateTransition(from, to) {
  const allowed = TRANSITIONS[from];
  if (!allowed || !allowed.includes(to)) {
    throw new InvalidTransitionError(from, to);
  }
  return true;
}

module.exports = {
  TRANSITIONS,
  InvalidTransitionError,
  validateTransition,
};
