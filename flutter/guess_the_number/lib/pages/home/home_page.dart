import 'package:flutter/material.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key, required this.title});

  final String title;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _nameController = TextEditingController();
  bool _ready = false;

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  void _startGame() {
    if (!_ready) return;
    FocusScope.of(context).unfocus();
    Navigator.of(context)
        .pushNamed('/guess', arguments: _nameController.text.trim());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.title)),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                Image.asset(
                  'assets/images/question.png',
                  width: 200,
                  height: 200,
                  excludeFromSemantics: true,
                ),
                const SizedBox(height: 40),
                TextField(
                  decoration: const InputDecoration(
                    labelText: '¿Cuál es tu nombre?',
                  ),
                  controller: _nameController,
                  textCapitalization: TextCapitalization.words,
                  textInputAction: TextInputAction.go,
                  onChanged: (value) {
                    setState(() => _ready = value.trim().isNotEmpty);
                  },
                  onSubmitted: (_) => _startGame(),
                ),
                const SizedBox(height: 80),
              ],
            ),
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Comenzar juego',
        onPressed: _ready ? _startGame : null,
        backgroundColor: _ready ? null : Colors.grey,
        child: const Icon(Icons.navigate_next),
      ),
    );
  }
}
