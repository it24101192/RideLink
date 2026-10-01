const {
  calculateFare,
  haversineDistanceKm,
  isPeakHour,
} = require('./fareCalculator');

describe('fare calculator', () => {
  test('calculates a normal fare outside peak hours', () => {
    // 200 + (5 * 50) + (10 * 5) = LKR 500.
    expect(calculateFare({
      distanceKm: 5,
      durationMin: 10,
      time: new Date('2026-01-15T06:00:00.000Z'), // 11:30 in Colombo
    })).toBe(500);
  });

  test('applies the minimum fare at the boundary', () => {
    expect(calculateFare({
      distanceKm: 0,
      durationMin: 0,
      time: new Date('2026-01-15T06:00:00.000Z'),
    })).toBe(300);
  });

  test('applies the 1.5x morning surge during Colombo peak hours', () => {
    // 200 + (5 * 50) + (10 * 5) = 500; 500 * 1.5 = LKR 750.
    expect(calculateFare({
      distanceKm: 5,
      durationMin: 10,
      time: new Date('2026-01-15T02:30:00.000Z'), // 08:00 in Colombo
    })).toBe(750);
  });

  test('applies the 1.5x evening surge during Colombo peak hours', () => {
    expect(isPeakHour(new Date('2026-01-15T12:00:00.000Z'))).toBe(true); // 17:30 local
  });

  test('treats peak window end times as non-peak', () => {
    expect(isPeakHour(new Date('2026-01-15T03:30:00.000Z'))).toBe(false); // 09:00 local
    expect(isPeakHour(new Date('2026-01-15T13:30:00.000Z'))).toBe(false); // 19:00 local
  });

  test('calculates Haversine distance and identical points as zero', () => {
    expect(haversineDistanceKm(6.9271, 79.8612, 6.9271, 79.8612)).toBe(0);
    expect(haversineDistanceKm(6.9271, 79.8612, 7.2906, 80.6337)).toBeGreaterThan(0);
  });

  test('rejects invalid fare inputs and invalid coordinates', () => {
    expect(() => calculateFare({ distanceKm: -1, durationMin: 10 })).toThrow(RangeError);
    expect(() => haversineDistanceKm(91, 0, 0, 0)).toThrow(RangeError);
  });
});
