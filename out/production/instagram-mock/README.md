# **Instagram Mock**

## **Description**
The **Messaging App** is a Java-based client-server application that enables users to interact through features like creating posts, stories, chatrooms, and sending messages. It leverages **Java RMI (Remote Method Invocation)** for remote communication and includes features like user registration, feed display, and a search functionality for posts.

This project demonstrates a scalable, distributed system using a load balancer to manage server loads effectively.

---

## **Features**
1. **User Registration and Login**:
    - Users register and connect to the least-loaded server.
    - Username is unique and identifies users.

2. **Messaging**:
    - Send messages to other users.
    - Chat in targeted chatrooms.

3. **Content Creation**:
    - Create regular posts or time-sensitive stories.
    - Stories expire after a user-defined visibility period.

4. **Feed and Interaction**:
    - View a feed containing posts and active stories.
    - Like or comment on posts.
    - Share posts with other users.

5. **Search Functionality**:
    - Search posts based on keywords, username, or time range.

6. **Follow System**:
    - Follow and unfollow other users.
    - Get notifications for interactions (e.g., likes, comments, or follows).

7. **Server Coordination**:
    - Multiple servers coordinated by a load balancer.
    - Servers share and synchronize state using RMI.

8. **Chatrooms**:
    - Create and join chatrooms.
    - Send messages visible to all members of a chatroom.

---

## **Technologies Used**
- **Programming Language**: Java
- **Remote Communication**: Java RMI (Remote Method Invocation)
- **Data Structures**: Collections Framework (e.g., `Map`, `List`)
- **Concurrency**: Multi-threading for managing server-side tasks.
- **Serialization**: For object persistence and data transfer.
- **Time Management**: `java.time.Instant` for timestamps and story expiration.
- **Client-Server Architecture**: Distributed system with a load balancer and multiple servers.

---

## **Setup and Execution**

## Database Setup

1. Ensure PostgreSQL is installed and running.
2. Create the database:
   ```bash
   createdb messaging_app
   ```
3. Import the schema:
   ```bash
   psql -U your_db_username -d messaging_app -f db/schema.sql
   ```
4. (Optional) Import seed data:
   ```bash
   psql -U your_db_username -d messaging_app -f db/seed_data.sql
   ```
5. Update the database credentials in the application (`DatabaseConnection` class) if necessary.

You’re ready to run the application!

### **Prerequisites**
1. **Java Development Kit (JDK)**: Version 8 or later.
2. **RMI Registry**:
    - Ensure that the RMI registry is running on the desired port (default: `1099`).

### **Steps to Run**
1. **Compile the Code**:
   ```bash
   javac *.java
2. **Start the Load Balancer**:
    ```bash
    java LoadBalancerImpl
3. **Start the Client**
    ```bash
   java MessagingClientImpl
<br>_Contributions are welcome! Submit issues or pull requests on the project's Git repository. 🌟_