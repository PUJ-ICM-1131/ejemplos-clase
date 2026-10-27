import 'package:flutter/material.dart';
import 'package:guess_the_number/pages/guessgame/guessgame_page.dart';
import 'package:guess_the_number/pages/home/home_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Adivina el número',
      theme: ThemeData(colorSchemeSeed: Colors.blue),
      home: const HomePage(title: 'Adivina el número - Inicio'),
      routes: {
        '/guess': (context) {
          final argument = ModalRoute.of(context)?.settings.arguments;
          final name = argument is String ? argument.trim() : '';
          return GuessGame(userName: name.isEmpty ? 'Jugador' : name);
        },
      },
      debugShowCheckedModeBanner: false,
    );
  }
}
