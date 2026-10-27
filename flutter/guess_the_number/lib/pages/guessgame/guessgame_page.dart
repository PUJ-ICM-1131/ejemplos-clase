import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:guess_the_number/models/guess_round.dart';

class GuessGame extends StatefulWidget {
  const GuessGame({super.key, this.userName = 'Jugador', this.round});

  final String userName;
  final GuessRound? round;

  @override
  State<GuessGame> createState() => _GuessGameState();
}

class _GuessGameState extends State<GuessGame> {
  final _guessController = TextEditingController();
  late final GuessRound _round = widget.round ?? GuessRound();
  String _message = '';
  String? _error;

  @override
  void dispose() {
    _guessController.dispose();
    super.dispose();
  }

  void _play() {
    if (_round.isComplete) return;
    final input = _guessController.text.trim();
    final result = _round.guess(input);
    setState(() {
      _error = null;
      switch (result) {
        case GuessResult.invalid:
          _error = 'Ingresa un número entero entre 1 y 100.';
        case GuessResult.tooLow:
          _message = '$input es menor que el número.';
        case GuessResult.tooHigh:
          _message = '$input es mayor que el número.';
        case GuessResult.correct:
          _message = '¡$input es el número ganador!';
        case GuessResult.finished:
          break;
      }
      if (result != GuessResult.invalid) _guessController.clear();
    });
    if (result == GuessResult.correct) {
      FocusScope.of(context).unfocus();
      _showSuccess();
    }
  }

  Future<void> _showSuccess() async {
    final playAgain = await showAdaptiveDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog.adaptive(
        title: const Text('¡Ganaste!'),
        content: Text('$_message\nIntentos: ${_round.attempts}'),
        actions: [
          _dialogAction(dialogContext, 'Volver al inicio', false),
          _dialogAction(dialogContext, 'Jugar de nuevo', true),
        ],
      ),
    );
    if (!mounted || playAgain == null) return;
    if (playAgain) {
      _newGame();
    } else {
      Navigator.of(context).popUntil((route) => route.isFirst);
    }
  }

  Widget _dialogAction(BuildContext context, String label, bool value) {
    void close() => Navigator.of(context).pop(value);
    final platform = Theme.of(context).platform;
    if (platform == TargetPlatform.iOS || platform == TargetPlatform.macOS) {
      return CupertinoDialogAction(onPressed: close, child: Text(label));
    }
    return TextButton(onPressed: close, child: Text(label));
  }

  void _newGame() {
    setState(() {
      _round.reset();
      _message = '';
      _error = null;
      _guessController.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Adivina el número')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  '${widget.userName}, adivina un número del 1 al 100',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                const SizedBox(height: 40),
                TextField(
                  decoration: InputDecoration(
                    labelText: 'Ingresa un número',
                    errorText: _error,
                  ),
                  enabled: !_round.isComplete,
                  controller: _guessController,
                  keyboardType: TextInputType.number,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _play(),
                ),
                const SizedBox(height: 20),
                ElevatedButton(
                  onPressed: _round.isComplete ? null : _play,
                  child: const Text('¡Jugar!'),
                ),
                const SizedBox(height: 20),
                Text(
                  'Mensaje: $_message',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 10),
                Text('Intentos: ${_round.attempts}'),
                const SizedBox(height: 80),
              ],
            ),
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nueva partida',
        onPressed: _round.isComplete ? _newGame : null,
        backgroundColor: _round.isComplete ? null : Colors.grey,
        child: const Icon(Icons.refresh),
      ),
    );
  }
}
