# Welcome to the Learning Outcomes Evaluation

Dear student,

Welcome to this Learning Outcomes Evaluation session. The goal is to assess your understanding and mastery of the learning outcomes for this semester as evidenced by your work that was submitted on your personal git account. Remember to answer each question thoroughly by referencing **Java** code and provide clear explanations where necessary.

Best regards,
Kay Berkling

## Ethics Section regarding generative and other forms of AI

The student acknowledges and agrees that the use of AI is strictly prohibited during this evaluation. By submitting this report, the student affirms that they have completed the form independently and without the assistance of any AI technologies. This agreement serves to ensure the integrity and authenticity of the students work, as well as their understanding of the learning outcomes. The student is also not allowed to copy paste statements that were prepared using AI into this document. 

Type your name here to agree:
Daniel Miuta
.................................. 


## Checklist before handing in your work

* [ ] Review the assignment requirements to ensure you have completed all the necessary tasks.
* [ ] Double-check your links and make sure that links lead to where you intended. Each answer should have links to work done by you in your own git repository. 
* [ ] Make sure you have at least 10 references to your project code (This is important evidence to prove that your project is substantial enough to support the learning outcome of object oriented design and coding within a larger piece of code.)
* [ ] Include comments to explain your referenced code and why it supports the learning outcome.
* [ ] Commit and push this markup file to your personal git repository and hand in the link and a soft-copy via email at the end of the designated time period.

Remember, this checklist is not exhaustive, but it should help you ensure that your work is complete, well-structured, and meets the required standards.

Good luck with your evaluation!

# Project (70%)

## Description (overall vision of the project)

It is a multiplayer PvP game where the users are able to create accounts and then they are able to create up to three clazzes (Samurai,Shinobi,Warrior).They can have battles one vs one or two versus one and so on.They are also able to count the kills.

## Link to your GIT Project

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/

## TECH STACK

Java Fx
Java Sockets
Meaven
SqlLite for database


## What did you achieve? (list each of the functionalities)
I have a end to end Java multiplayer game with registration/login, clazz creation,clazz selection. I also have the option of switching the clazz during the game. I also can see Top N killers.



## Pick 3 of the functionalities
#### for each of the 3 functionalities link to the class that is responsible for it and explain the role of this and other classes in combination to fulfill the functionality

### 1
Registration. I performe the registration in AuthManager.java class inside of register method on the line 35.Before registration i check if the username is unique.
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/2c7a3b7db3e06f20065717c3d1feb812ebbb1f95/src/main/java/com/codebrawl/auth/AuthManager.java#L35

### 2
Fighter creation. I have Fighter.java class which has the role to create the clazz based on some attributes that are coming from extended classes like Samurai,Shinobi,Warrior.

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Fighter.java

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Samurai.java
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Shinobi.java
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Warrior.java

### 3
I used PasswordUtil.java for checking the password inside of login method from AuthManager class.

PasswordUtil.java:

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/auth/PasswordUtil.java

Login method on line 59:

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/2c7a3b7db3e06f20065717c3d1feb812ebbb1f95/src/main/java/com/codebrawl/auth/AuthManager.java#L59

| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|           9            |                               |
|||

## Learning Outcomes

| Exam Question | Total Achievable Points | Points Reached During Grading |
|---------------|------------------------|-------------------------------|
| Q1.Algorithms    |           4            |                               |
| Q2.Data types    |           4            |                               |
| Q3.Complex Data Structures |  4            |                               |
| Q4.Concepts of OOP |          6            |                               |
| Q5.OO Design     |           6            |                               |
| Q6.Testing       |           3            |                               |
| Q7.Operator/Method Overloading | 4 |                               |
| Q8.Templates/Generics |       6            |                               |
| Q9.Class libraries |          4            |                               |


## Evaluation Questions

Please answer the following questions to the best of your ability to show your understanding of the learning outcomes. Please provide examples from your project code to support your answers.


## Evaluation Material
All questions require an explanation and a link to your project on git. 

### Q1. Algorithms

Algorithms are manifold and Java can be used to program these. Examples are sorting or search strategies but also mathematical calculations. Please refer to **two** areas within your project with a link, where you have coded an algorithm. Do not make reference to code written for other classes, like theoretical informatics. Explain the algorithm.

In my project i have an algorithm inside of tick method from  World.java class for movement integration on line 86
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/2c7a3b7db3e06f20065717c3d1feb812ebbb1f95/src/main/java/com/codebrawl/realtime/World.java#L86

I also have my own rep with some algorithms such binary search
https://github.com/DanielMiuta24/JavaClass3/blob/main/algorithms/BinarySearch.java


Bubble sort 
https://github.com/DanielMiuta24/JavaClass3/blob/main/algorithms/BubbleSort.java





| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|           4            |                               |
|||

### Q2. Data types

Please **explain** the concept of data types and provide examples with links of different data types in Java. Typical data types in java are int, double, float, char, boolean, long, short, byte, String, and arrays. Please provide one example with a link for each of the **four** following data types in your code by linking to it. 

A boolean type in java is a data type that holds 1 or 0
I used it in the ClientApp.java class to keep the  input flags on line 29.

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/ui/ClientApp.java#L29C1-L29C72

A int type in java can save values such 1,2,3(integer values). I used them for hp in World class on line 88.

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/realtime/World.java#L88

A String in java is a data type where we can store text for example. I used it in my project for saving the clazz (Samurai and so on)
 Line 32
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/model/Fighter.java#L4C4-L4C32







| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|           4             |                               |
|||


### Q3. Complex Data Structures

Examples of complex data structures in java are ArrayList, HashMap, HashSet, LinkedList, and TreeMap. Please provide **two** examples with a link of how you have used these complex data structures in your code and **explain** why you have chosen these data structures.


I used ArrayList in my project for selecting the calzzes from database. For example in AuthManage on line 78 
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/2c7a3b7db3e06f20065717c3d1feb812ebbb1f95/src/main/java/com/codebrawl/auth/AuthManager.java#L78

I also used HashMap in the World class on line 33 to track the each connected player by their session
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/realtime/World.java#L33

  

| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|           4             |                               |
|||

### Q4. Concepts of OOP
Concepts of OOP are the basic building blocks of object-oriented programming, such as classes, objects, methods, and attributes. 
**Explain** HOW and WHY your **project** demonstrates the use of OOP by using all of the following concepts:
* Classes/Objects
* Methods
* Attributes 
**Link** to the code in your project that demonstrates what you have explained above.

In my project i have classes like Figher where i define the structure of the player. I also have extended classes like Samurai,Shinobbi and Warrior where i define exactly what kind of attributes each clazz has.

https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Fighter.java
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Samurai.java
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Shinobi.java
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/main/java/com/codebrawl/model/Warrior.java

In fighter class i have attributes like clazz name ,hp,mp,guarding.I use them to define the sub-clazzes

I also have the method like spawnPlayer in the World class. I used it for spawning the player when  joins in the game.
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/realtime/World.java#L40


| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|             6           |                               |
|||

### Q5. OO Design
Please showcase **two** areas by linking to them where you have used object orientation and **explain** the advantage that object oriented code brings to the application or the problem that your code is addressing.
Examples in java of good oo design are encapsulation, inheritance, polymorphism, and abstraction. 

I used  inheritance in Samurai.java,Shinobbi.java,Warrior.java. The advantage is we can use the attributes and everything that is inside of Fighter class in order to create a clazz.
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/model/Samurai.java#L3

I also have polymorphism inside of World class on line 88
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/realtime/World.java#L88

And also abstraction in the world.java class on line 109
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/realtime/World.java#L109



| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|              6          |                               |
|||


### Q6. Testing
Java code is tested by using JUnit. Please **explain** how you have used JUnit in your project and provide a link to the code where you have used JUnit. If you tested without JUnit, please explain how you tested your code. Why did you test that particular part. How did you test that part.

I have done a JUnit for AuthManager where i test  the registration/login and everything that is inside of AuthManager class
https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/main/src/test/java/com/codebrawl/AuthManagerTest.java

| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|         3               |                               |
|||

### Q7. Operator/Method Overloading
An example of operator overloading is the "+" operator that can be used to add two numbers or concatenate two strings. An example of method overloading is having two methods with the same name but different parameters. Please provide **one** example with a link of how you have used operator or method overloading in your code and **explain** why you have chosen this method of coding.
Do not use "+" for your answer. 

 I used operator overloading in the World.java on line 145

*your text*


https://github.com/DanielMiuta24/Multiplayer-PvP-Fighting-Game-Java-Project/blob/85dda2d2ed6ac7575de411f038414461a14d3e7f/src/main/java/com/codebrawl/realtime/World.java#L145C21-L145C70

| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|          4              |                               |
|||


### Q8. Templates/Generics
Generics in java are used to create classes, interfaces, and methods that operate on objects of specified types. Please provide **two** example with a link of how you have used generics in your code and **explain** why you have chosen to use generics. 


*your text*

| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|           6             |                               |
|||

### Q9. Class Libraries
Examples of class libraries in java are the Java Standard Library, JavaFX, Apache Commons, JUnit, Log4j, Jackson, Guava, Joda-Time, Hibernate, Spring, Maven, and many more. Please provide **one** example with a link of how you have used a class library in your **project** code and **explain** why you have chosen to use this class library. 

*your text*

| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|            4            |                               |
|||


# Creativity (10%)
Which one did you choose: 

* [ ] Web Interface with Design
* [ ] Database Connected
* [ ] Multithreading
* [ ] File I/O
* [ ] API
* [ ] Deployment

Please **explain** which one of the above you worked with, link to the area in your code and why you chose to use that approach. 

*your text*



| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|            10          |                               |
|||


# Speed Coding (20%)
Please enter **three** Links to your pair programming session GITs and name your partner. 

Topic: Calculator

1. https://github.com/Roiczone/Pair-programming-1

Topic: Online Shop

2. https://github.com/Roiczone/Pairprogramming_2

Topic:Citizen Management System
3. https://github.com/Roiczone/Pairprogramming_3



| Total Achievable Points | Points Reached During Grading |
|------------------------|-------------------------------|
|                        |                               |
|            15            |                               |
|||




