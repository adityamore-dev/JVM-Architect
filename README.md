# JVM Architect

An interactive Java Swing game designed to visualize JVM memory management concepts through gameplay.

The project simulates object allocation, object aging, generational memory spaces, and garbage collection mechanics while displaying real JVM runtime memory statistics.

---

## Features

- Object allocation simulation
- Young Generation (Eden) visualization
- Survivor Space visualization
- Old Generation (Tenured) visualization
- Object aging and promotion mechanics
- Garbage Collection inspired gameplay
- Runtime heap monitoring using Java Runtime API
- Particle effect system
- Combo and scoring system
- Boss battle mechanics
- Persistent high score storage
- Pause and game over states

---

## Technologies Used

- Java
- Java Swing
- Java 2D Graphics (Graphics2D)
- Collections Framework
- Multithreading
- AtomicInteger
- CopyOnWriteArrayList
- File Handling
- Runtime API

---

## JVM Concepts Demonstrated

This project is an educational visualization and does not implement the actual JVM.

Concepts represented in gameplay:

- Young Generation (Eden)
- Survivor Space
- Old Generation (Tenured)
- Object Aging
- Object Promotion
- Garbage Collection
- Heap Monitoring

---

## Concurrency Concepts Used

- Runnable
- Custom Game Loop Thread
- volatile variables
- AtomicInteger
- Thread-safe collections using CopyOnWriteArrayList

---

## File Persistence

High scores are stored locally using file I/O.

Files used:

- highscore.dat

---

## How To Run

Compile: ```javac com/jvm/game/*.java```

Run: ```java com.jvm.game.GameWindow```

---

## Learning Outcomes

This project helped strengthen understanding of:

- Core Java
- Object-Oriented Programming
- Swing GUI Development
- Java Collections Framework
- Multithreading
- Graphics Rendering
- File Handling
- JVM Memory Concepts

---

## Author

Aditya Suresh  More
