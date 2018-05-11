# MessengerOne
A command line whatsapp clone.

I had heard a lot about Kotlin (JetBrains' new JVM language) over the past few months and was looking for an excuse to try it out to see whether it lived up to all the hype. What better way to take it for a spin than with a healthy dose of multithreading and socket programming?

This is a server for an instant messaging service. Users can log in and send instant messages to other users. If the recipient is not connected the message is kept in a queue in memory on the server and delivered when that user connects again. Messages are not kept on the server after they have been delivered.

Even though this was just a practice project, I decided to build it performance consciously. It should theoretically be able to handle large numbers of users and a high velocity of messages, user sign-ups, logins and logouts, although I have not tested this.

Nothing is saved to disk, all users and messages are stored in memory and dissapear when the server is terminated. I wrote a simple command line client application (in both Kotlin and Java) to test the functionality of the server.

# My thoughts on Kotlin
I tried to write idiomatic Kotlin code (JetBrains have helpfully put up a list of "idioms" on their website) because I assumed that I would get the most benefit from the language if I used it the way they had designed it to be used. During the course of this short project, I came to like the language. In particular the optional types, encouraged use of constants and the short-hand class definitions made the code much cleaner, easier to write and enforced proper, robust handling of edge cases.

All-in-all it was easy to learn, fun to write and I believe it led to me writing better code. That's a yes from me.
