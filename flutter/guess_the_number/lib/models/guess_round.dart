import 'dart:math';

enum GuessResult { invalid, tooLow, tooHigh, correct, finished }

class GuessRound {
  GuessRound({Random? random}) : _random = random ?? Random() {
    reset();
  }

  final Random _random;
  late int _target;
  int attempts = 0;
  bool isComplete = false;

  GuessResult guess(String input) {
    if (isComplete) return GuessResult.finished;
    final value = int.tryParse(input.trim());
    if (value == null || value < 1 || value > 100) {
      return GuessResult.invalid;
    }
    attempts++;
    if (value < _target) return GuessResult.tooLow;
    if (value > _target) return GuessResult.tooHigh;
    isComplete = true;
    return GuessResult.correct;
  }

  void reset() {
    _target = _random.nextInt(100) + 1;
    attempts = 0;
    isComplete = false;
  }
}
