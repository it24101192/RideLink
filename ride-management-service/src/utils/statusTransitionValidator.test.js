const {
  TRANSITIONS,
  InvalidTransitionError,
  validateTransition,
} = require('./statusTransitionValidator');

describe('status transition validator', () => {
  test.each([
    ['REQUESTED', 'ASSIGNED'],
    ['REQUESTED', 'CANCELLED'],
    ['ASSIGNED', 'ACCEPTED'],
    ['ASSIGNED', 'CANCELLED'],
    ['ACCEPTED', 'IN_PROGRESS'],
    ['ACCEPTED', 'CANCELLED'],
    ['IN_PROGRESS', 'COMPLETED'],
  ])('allows %s -> %s', (from, to) => {
    expect(validateTransition(from, to)).toBe(true);
  });

  test.each([
    ['REQUESTED', 'ACCEPTED'],
    ['REQUESTED', 'IN_PROGRESS'],
    ['REQUESTED', 'COMPLETED'],
    ['ASSIGNED', 'REQUESTED'],
    ['ASSIGNED', 'IN_PROGRESS'],
    ['ASSIGNED', 'COMPLETED'],
    ['ACCEPTED', 'REQUESTED'],
    ['ACCEPTED', 'ASSIGNED'],
    ['ACCEPTED', 'COMPLETED'],
    ['IN_PROGRESS', 'CANCELLED'],
    ['IN_PROGRESS', 'ACCEPTED'],
    ['COMPLETED', 'CANCELLED'],
    ['COMPLETED', 'REQUESTED'],
    ['CANCELLED', 'REQUESTED'],
    ['CANCELLED', 'ASSIGNED'],
    ['UNKNOWN', 'REQUESTED'],
  ])('rejects %s -> %s', (from, to) => {
    expect(() => validateTransition(from, to)).toThrow(InvalidTransitionError);
  });

  test('error has a stable code, status, and transition details', () => {
    try {
      validateTransition('REQUESTED', 'COMPLETED');
      throw new Error('Expected validateTransition to throw');
    } catch (error) {
      expect(error).toBeInstanceOf(InvalidTransitionError);
      expect(error).toMatchObject({
        name: 'InvalidTransitionError',
        code: 'INVALID_STATUS_TRANSITION',
        statusCode: 400,
        from: 'REQUESTED',
        to: 'COMPLETED',
      });
      expect(error.message).toBe('Invalid Status Transition: REQUESTED -> COMPLETED');
    }
  });

  test('exposes the complete transition map', () => {
    expect(TRANSITIONS).toEqual({
      REQUESTED: ['ASSIGNED', 'CANCELLED'],
      ASSIGNED: ['ACCEPTED', 'CANCELLED'],
      ACCEPTED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['COMPLETED'],
      COMPLETED: [],
      CANCELLED: [],
    });
  });
});
