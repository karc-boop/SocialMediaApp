import java.sql.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class NewSocialMediaApp {

  private Connection conn;
  private Scanner scanner;
  private int currentUserId = -1; // -1 indicates no user is logged in
  private String currentUsername = null;

  public NewSocialMediaApp() {
    scanner = new Scanner(System.in);
  }

  // Initialize database connection
  private boolean connect() {
    System.out.print("Please enter your MySQL username: ");
    String dbUser = scanner.nextLine();

    System.out.print("Please enter your MySQL password: ");
    String dbPassword = scanner.nextLine();

    String dbUrl = "jdbc:mysql://localhost:3306/social_media?useSSL=false&serverTimezone=UTC";

    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
      System.out.println("Successfully connected to the Social Media database!");
      return true;
    } catch (SQLException e) {
      System.out.println("Failed to connect to the database:");
      System.out.println(e.getMessage());
      return false;
    }
  }

  // Close database connection
  private void disconnect() {
    try {
      if (conn != null && !conn.isClosed()) {
        conn.close();
        System.out.println("Disconnected from the database.");
      }
    } catch (SQLException e) {
      System.out.println("Error disconnecting from the database:");
      System.out.println(e.getMessage());
    }
  }

  // Main menu for unauthenticated users
  private void unauthenticatedMenu() {
    while (currentUserId == -1) {
      System.out.println("\n=== Welcome to Social Media App ===");
      System.out.println("1. Sign Up");
      System.out.println("2. Sign In");
      System.out.println("3. Exit");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          signUp();
          break;
        case "2":
          signIn();
          break;
        case "3":
          System.out.println("Exiting application...");
          disconnect();
          System.exit(0);
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Main menu for authenticated users
  private void authenticatedMenu() {
    while (currentUserId != -1) {
      System.out.println("\n=== Social Media Application ===");
      System.out.println("1. Users");
      System.out.println("2. Posts");
      System.out.println("3. Comments");
      System.out.println("4. Friendships");
      System.out.println("5. Likes");
      System.out.println("6. Messages");
      System.out.println("7. Notifications");
      System.out.println("8. Log Out");
      System.out.println("9. Delete Account");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          usersMenu();
          break;
        case "2":
          postsMenu();
          break;
        case "3":
          commentsMenu();
          break;
        case "4":
          friendshipsMenu();
          break;
        case "5":
          likesMenu();
          break;
        case "6":
          messagesMenu();
          break;
        case "7":
          notificationsMenu();
          break;
        case "8":
          logOut();
          break;
        case "9":
          deleteAccount();
          break;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Sign Up
  private void signUp() {
    try {
      System.out.println("\n--- Sign Up ---");
      System.out.print("Enter Username: ");
      String username = scanner.nextLine();

      System.out.print("Enter Email: ");
      String email = scanner.nextLine();

      System.out.print("Enter Password: ");
      String password = scanner.nextLine();
      String passwordHash = hashPassword(password); // Implement proper hashing in production

      System.out.print("Enter Name: ");
      String name = scanner.nextLine();

      System.out.print("Enter Bio: ");
      String bio = scanner.nextLine();

      System.out.print("Enter Profile Picture URL: ");
      String profilePicture = scanner.nextLine();

      String sql = "{CALL AddUser(?, ?, ?, ?, ?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setString(1, username);
      stmt.setString(2, email);
      stmt.setString(3, passwordHash);
      stmt.setString(4, name);
      stmt.setString(5, bio);
      stmt.setString(6, profilePicture);

      stmt.execute();
      System.out.println("User registered successfully. You can now sign in.");
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error during sign up:");
      System.out.println(e.getMessage());
    }
  }

  // Sign In
  private void signIn() {
    try {
      System.out.println("\n--- Sign In ---");
      System.out.print("Enter Username: ");
      String username = scanner.nextLine();

      System.out.print("Enter Password: ");
      String password = scanner.nextLine();
      String passwordHash = hashPassword(password);

      String query = "SELECT UserID FROM users WHERE Username = ? AND PasswordHash = ?";
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setString(1, username);
      stmt.setString(2, passwordHash);
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        currentUserId = rs.getInt("UserID");
        currentUsername = username;
        System.out.println("Sign in successful! Welcome, " + currentUsername + ".");
      } else {
        System.out.println("Invalid username or password.");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) { // MySQL error code for duplicate entry
        if (e.getMessage().contains("users.Email_Unique")) {
          System.out.println("The email you entered is already associated with an existing account.");
          System.out.println("Please signing in or use a different email.");
        } else if (e.getMessage().contains("users.Username_Unique")) {
          System.out.println("The username you entered is already taken.");
          System.out.println("Please use a new username.");
        }
      } else {
        System.out.println("Error during sign up:");
        System.out.println(e.getMessage());
      }
    }
  }

  // Log Out
  private void logOut() {
    currentUserId = -1;
    currentUsername = null;
    System.out.println("Logged out successfully.");
  }

  // Delete Account
  private void deleteAccount() {
    try {
      System.out.print("Are you sure you want to delete your account? This action cannot be undone. (yes/no): ");
      String confirmation = scanner.nextLine().trim().toLowerCase();

      if (confirmation.equals("yes")) {
        String sql = "DELETE FROM users WHERE UserID = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, currentUserId);

        int rowsAffected = stmt.executeUpdate();
        if (rowsAffected > 0) {
          System.out.println("Your account has been deleted successfully.");
          currentUserId = -1;
          currentUsername = null;
        } else {
          System.out.println("Error deleting your account.");
        }
        stmt.close();
      } else {
        System.out.println("Account deletion canceled.");
      }
    } catch (SQLException e) {
      System.out.println("Error deleting account:");
      System.out.println(e.getMessage());
    }
  }

  // Users Menu
  private void usersMenu() {
    while (true) {
      System.out.println("\n--- Users Menu ---");
      System.out.println("1. View My Profile");
      System.out.println("2. Update My Profile");
      System.out.println("3. View All Users");
      System.out.println("4. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          viewProfile();
          break;
        case "2":
          updateProfile();
          break;
        case "3":
          viewAllUsers();
          break;
        case "4":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // View My Profile
  private void viewProfile() {
    try {
      String query = "SELECT UserID, Username, Email, Name, Bio, ProfilePicture, CreatedAt, UpdatedAt FROM users WHERE UserID = ?";
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, currentUserId);
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        System.out.println("\n--- My Profile ---");
        System.out.println("UserID: " + rs.getInt("UserID"));
        System.out.println("Username: " + rs.getString("Username"));
        System.out.println("Email: " + rs.getString("Email"));
        System.out.println("Name: " + rs.getString("Name"));
        System.out.println("Bio: " + rs.getString("Bio"));
        System.out.println("Profile Picture: " + rs.getString("ProfilePicture"));
        System.out.println("Created At: " + rs.getTimestamp("CreatedAt"));
        System.out.println("Updated At: " + rs.getTimestamp("UpdatedAt"));
      } else {
        System.out.println("Error fetching your profile.");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error viewing profile:");
      System.out.println(e.getMessage());
    }
  }

  // Update My Profile
  private void updateProfile() {
    try {
      System.out.println("\n--- Update My Profile ---");
      System.out.print("Enter new Username: ");
      String username = scanner.nextLine();

      System.out.print("Enter new Email: ");
      String email = scanner.nextLine();

      System.out.print("Enter new Password: ");
      String password = scanner.nextLine();
      String passwordHash = hashPassword(password); // Implement proper hashing in production

      System.out.print("Enter new Name: ");
      String name = scanner.nextLine();

      System.out.print("Enter new Bio: ");
      String bio = scanner.nextLine();

      System.out.print("Enter new Profile Picture URL: ");
      String profilePicture = scanner.nextLine();

      String sql = "{CALL UpdateUserProfile(?, ?, ?, ?, ?, ?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, currentUserId);
      stmt.setString(2, username);
      stmt.setString(3, email);
      stmt.setString(4, passwordHash);
      stmt.setString(5, name);
      stmt.setString(6, bio);
      stmt.setString(7, profilePicture);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Profile updated successfully.");
      } else {
        System.out.println("Error updating your profile.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error updating profile:");
      System.out.println(e.getMessage());
    }
  }

  // View All Users (excluding self)
  private void viewAllUsers() {
    try {
      String query = "SELECT UserID, Username, Name FROM users WHERE UserID != ?";
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, currentUserId);
      ResultSet rs = stmt.executeQuery();

      List<String> usernames = new ArrayList<>();
      System.out.println("\n--- All Users ---");
      while (rs.next()) {
        System.out.println("UserID: " + rs.getInt("UserID") + ", Username: " + rs.getString("Username") + ", Name: " + rs.getString("Name"));
        usernames.add(rs.getString("Username"));
      }

      if (usernames.isEmpty()) {
        System.out.println("No other users found.");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error viewing all users:");
      System.out.println(e.getMessage());
    }
  }

  // Posts Menu
  private void postsMenu() {
    while (true) {
      System.out.println("\n--- Posts Menu ---");
      System.out.println("1. Create Post");
      System.out.println("2. Read Posts");
      System.out.println("3. Update Post");
      System.out.println("4. Delete Post");
      System.out.println("5. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          createPost();
          break;
        case "2":
          readPosts();
          break;
        case "3":
          updatePost();
          break;
        case "4":
          deletePost();
          break;
        case "5":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Create a new post
  private void createPost() {
    try {
      System.out.print("Enter Content: ");
      String content = scanner.nextLine();

      System.out.print("Enter Media Type (text/image/video): ");
      String mediaType = scanner.nextLine().toLowerCase();
      if (!mediaType.equals("text") && !mediaType.equals("image") && !mediaType.equals("video")) {
        System.out.println("Invalid Media Type.");
        return;
      }

      System.out.print("Enter Media URL (or leave blank): ");
      String mediaURL = scanner.nextLine();
      if (mediaURL.isEmpty()) {
        mediaURL = null;
      }

      String sql = "{CALL AddPost(?, ?, ?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, currentUserId);
      stmt.setString(2, content);
      stmt.setString(3, mediaType);
      stmt.setString(4, mediaURL);

      stmt.execute();
      System.out.println("Post created successfully.");
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error creating post:");
      System.out.println(e.getMessage());
    }
  }

  // Read and display posts (own and friends')
  private void readPosts() {
    try {
      String query = "SELECT p.PostID, p.UserID, u.Username, p.Content, p.MediaType, p.MediaURL, p.Timestamp " +
              "FROM posts p " +
              "JOIN users u ON p.UserID = u.UserID " +
              "LEFT JOIN friendships f ON (f.UserID1 = ? AND f.UserID2 = p.UserID) " +
              "OR (f.UserID2 = ? AND f.UserID1 = p.UserID) " +
              "WHERE p.UserID = ? OR f.UserID1 IS NOT NULL OR f.UserID2 IS NOT NULL " +
              "ORDER BY p.Timestamp DESC";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, currentUserId);
      stmt.setInt(2, currentUserId);
      stmt.setInt(3, currentUserId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Posts ---");
      while (rs.next()) {
        System.out.println("PostID: " + rs.getInt("PostID"));
        System.out.println("Author: " + rs.getString("Username"));
        System.out.println("Content: " + rs.getString("Content"));
        System.out.println("Media Type: " + rs.getString("MediaType"));
        System.out.println("Media URL: " + rs.getString("MediaURL"));
        System.out.println("Timestamp: " + rs.getTimestamp("Timestamp"));
        System.out.println("---------------------------");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error reading posts:");
      System.out.println(e.getMessage());
    }
  }

  // Update a post (only own)
  private void updatePost() {
    try {
      System.out.print("Enter PostID to update: ");
      int postId = Integer.parseInt(scanner.nextLine());

      // Verify ownership
      if (!isPostOwner(postId)) {
        System.out.println("You can only update your own posts.");
        return;
      }

      System.out.print("Enter new Content: ");
      String content = scanner.nextLine();

      System.out.print("Enter new Media Type (text/image/video): ");
      String mediaType = scanner.nextLine().toLowerCase();
      if (!mediaType.equals("text") && !mediaType.equals("image") && !mediaType.equals("video")) {
        System.out.println("Invalid Media Type.");
        return;
      }

      System.out.print("Enter new Media URL (or leave blank): ");
      String mediaURL = scanner.nextLine();
      if (mediaURL.isEmpty()) {
        mediaURL = null;
      }

      String sql = "{CALL UpdatePost(?, ?, ?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, postId);
      stmt.setString(2, content);
      stmt.setString(3, mediaType);
      stmt.setString(4, mediaURL);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Post updated successfully.");
      } else {
        System.out.println("Error updating post.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error updating post:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid PostID format.");
    }
  }

  // Delete a post (only own)
  private void deletePost() {
    try {
      System.out.print("Enter PostID to delete: ");
      int postId = Integer.parseInt(scanner.nextLine());

      // Verify ownership
      if (!isPostOwner(postId)) {
        System.out.println("You can only delete your own posts.");
        return;
      }

      String sql = "{CALL DeletePost(?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, postId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Post deleted successfully.");
      } else {
        System.out.println("Error deleting post.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error deleting post:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid PostID format.");
    }
  }

  // Check if the current user owns the post
  private boolean isPostOwner(int postId) throws SQLException {
    String query = "SELECT UserID FROM posts WHERE PostID = ?";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, postId);
    ResultSet rs = stmt.executeQuery();

    boolean isOwner = false;
    if (rs.next()) {
      int ownerId = rs.getInt("UserID");
      if (ownerId == currentUserId) {
        isOwner = true;
      }
    }

    rs.close();
    stmt.close();
    return isOwner;
  }

  // Comments Menu
  private void commentsMenu() {
    while (true) {
      System.out.println("\n--- Comments Menu ---");
      System.out.println("1. Comment on a Post");
      System.out.println("2. Read Comments on a Post");
      System.out.println("3. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          createComment();
          break;
        case "2":
          readComments();
          break;
        case "3":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Create a new comment (only on own or friends' posts)
  private void createComment() {
    try {
      System.out.print("Enter PostID to comment on: ");
      int postId = Integer.parseInt(scanner.nextLine());

      // Verify access
      if (!canAccessPost(postId)) {
        System.out.println("You can only comment on your own posts or your friends' posts.");
        return;
      }

      System.out.print("Enter Content: ");
      String content = scanner.nextLine();

      String sql = "{CALL AddComment(?, ?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, postId);
      stmt.setInt(2, currentUserId);
      stmt.setString(3, content);

      stmt.execute();
      System.out.println("Comment added successfully.");
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error adding comment:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid PostID format.");
    }
  }

  // Read and display comments on a post (only own or friends' posts)
  private void readComments() {
    try {
      System.out.print("Enter PostID to view comments: ");
      int postId = Integer.parseInt(scanner.nextLine());

      // Verify access
      if (!canAccessPost(postId)) {
        System.out.println("You can only view comments on your own posts or your friends' posts.");
        return;
      }

      String query = "SELECT c.CommentID, c.UserID, u.Username, c.Content, c.Timestamp " +
              "FROM comments c " +
              "JOIN users u ON c.UserID = u.UserID " +
              "WHERE c.PostID = ? " +
              "ORDER BY c.Timestamp ASC";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, postId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Comments on PostID: " + postId + " ---");
      while (rs.next()) {
        System.out.println("CommentID: " + rs.getInt("CommentID"));
        System.out.println("Author: " + rs.getString("Username"));
        System.out.println("Content: " + rs.getString("Content"));
        System.out.println("Timestamp: " + rs.getTimestamp("Timestamp"));
        System.out.println("---------------------------");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error reading comments:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid PostID format.");
    }
  }

  // Check if the current user can access the post (own or friends')
  private boolean canAccessPost(int postId) throws SQLException {
    String query = "SELECT p.UserID FROM posts p " +
            "LEFT JOIN friendships f ON (f.UserID1 = ? AND f.UserID2 = p.UserID) " +
            "OR (f.UserID2 = ? AND f.UserID1 = p.UserID) " +
            "WHERE p.PostID = ? AND (p.UserID = ? OR f.UserID1 IS NOT NULL OR f.UserID2 IS NOT NULL)";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, currentUserId);
    stmt.setInt(2, currentUserId);
    stmt.setInt(3, postId);
    stmt.setInt(4, currentUserId);
    ResultSet rs = stmt.executeQuery();

    boolean canAccess = rs.next();

    rs.close();
    stmt.close();
    return canAccess;
  }

  // Friendships Menu
  private void friendshipsMenu() {
    while (true) {
      System.out.println("\n--- Friendships Menu ---");
      System.out.println("1. Add Friend");
      System.out.println("2. View Friends");
      System.out.println("3. Remove Friend");
      System.out.println("4. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          addFriend();
          break;
        case "2":
          viewFriends();
          break;
        case "3":
          removeFriend();
          break;
        case "4":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Add a friend
  private void addFriend() {
    try {
      System.out.print("Enter the username of the user you want to add as a friend: ");
      String friendUsername = scanner.nextLine();

      // Get friend's UserID
      int friendUserId = getUserIdByUsername(friendUsername);
      if (friendUserId == -1) {
        System.out.println("User not found.");
        return;
      }

      if (friendUserId == currentUserId) {
        System.out.println("You cannot add yourself as a friend.");
        return;
      }

      // Check if already friends
      if (areFriends(currentUserId, friendUserId)) {
        System.out.println("You are already friends with this user.");
        return;
      }

      // Add friendship
      String sql = "{CALL AddFriendship(?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, currentUserId);
      stmt.setInt(2, friendUserId);

      stmt.execute();
      System.out.println("Friend added successfully.");
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error adding friend:");
      System.out.println(e.getMessage());
    }
  }

  // View friends
  private void viewFriends() {
    try {
      String query = "SELECT u.UserID, u.Username, u.Name " +
              "FROM friendships f " +
              "JOIN users u ON (u.UserID = f.UserID1 AND f.UserID2 = ?) " +
              "OR (u.UserID = f.UserID2 AND f.UserID1 = ?)";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, currentUserId);
      stmt.setInt(2, currentUserId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Your Friends ---");
      boolean hasFriends = false;
      while (rs.next()) {
        hasFriends = true;
        System.out.println("UserID: " + rs.getInt("UserID") + ", Username: " + rs.getString("Username") + ", Name: " + rs.getString("Name"));
      }

      if (!hasFriends) {
        System.out.println("You have no friends added.");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error viewing friends:");
      System.out.println(e.getMessage());
    }
  }

  // Remove a friend
  private void removeFriend() {
    try {
      System.out.print("Enter the username of the friend you want to remove: ");
      String friendUsername = scanner.nextLine();

      // Get friend's UserID
      int friendUserId = getUserIdByUsername(friendUsername);
      if (friendUserId == -1) {
        System.out.println("User not found.");
        return;
      }

      if (!areFriends(currentUserId, friendUserId)) {
        System.out.println("You are not friends with this user.");
        return;
      }

      String sql = "{CALL DeleteFriendship(?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, currentUserId);
      stmt.setInt(2, friendUserId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Friend removed successfully.");
      } else {
        System.out.println("Error removing friend.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error removing friend:");
      System.out.println(e.getMessage());
    }
  }

  // Check if two users are friends
  private boolean areFriends(int userId1, int userId2) throws SQLException {
    String query = "SELECT * FROM friendships WHERE (UserID1 = ? AND UserID2 = ?) OR (UserID1 = ? AND UserID2 = ?)";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, Math.min(userId1, userId2));
    stmt.setInt(2, Math.max(userId1, userId2));
    stmt.setInt(3, Math.max(userId1, userId2));
    stmt.setInt(4, Math.min(userId1, userId2));
    ResultSet rs = stmt.executeQuery();

    boolean friends = rs.next();

    rs.close();
    stmt.close();
    return friends;
  }

  // Get UserID by Username
  private int getUserIdByUsername(String username) throws SQLException {
    String query = "SELECT UserID FROM users WHERE Username = ?";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setString(1, username);
    ResultSet rs = stmt.executeQuery();

    int userId = -1;
    if (rs.next()) {
      userId = rs.getInt("UserID");
    }

    rs.close();
    stmt.close();
    return userId;
  }

  // Likes Menu
  private void likesMenu() {
    while (true) {
      System.out.println("\n--- Likes Menu ---");
      System.out.println("1. Like a Post");
      System.out.println("2. Like a Comment");
      System.out.println("3. Read Post Likes");
      System.out.println("4. Read Comment Likes");
      System.out.println("5. Delete Post Like");
      System.out.println("6. Delete Comment Like");
      System.out.println("7. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          likePost();
          break;
        case "2":
          likeComment();
          break;
        case "3":
          readPostLikes();
          break;
        case "4":
          readCommentLikes();
          break;
        case "5":
          deletePostLike();
          break;
        case "6":
          deleteCommentLike();
          break;
        case "7":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Like a post (only own or friends' posts)
  private void likePost() {
    try {
      System.out.print("Enter PostID to like: ");
      int postId = Integer.parseInt(scanner.nextLine());

      // Verify access
      if (!canAccessPost(postId)) {
        System.out.println("You can only like your own posts or your friends' posts.");
        return;
      }

      String sql = "{CALL AddPostLike(?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, postId);
      stmt.setInt(2, currentUserId);

      stmt.execute();
      System.out.println("Post liked successfully.");
      stmt.close();
    } catch (SQLException e) {
      if (e.getErrorCode() == 45000) { // Triggered error
        System.out.println(e.getMessage());
      } else if (e.getErrorCode() == 1062) { // Duplicate entry
        System.out.println("You have already liked this post.");
      } else {
        System.out.println("Error liking post:");
        System.out.println(e.getMessage());
      }
    } catch (NumberFormatException e) {
      System.out.println("Invalid PostID format.");
    }
  }

  // Like a comment (only own or friends' comments)
  private void likeComment() {
    try {
      System.out.print("Enter CommentID to like: ");
      int commentId = Integer.parseInt(scanner.nextLine());

      // Verify access
      if (!canAccessComment(commentId)) {
        System.out.println("You can only like your own comments or your friends' comments.");
        return;
      }

      String sql = "{CALL AddCommentLike(?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, commentId);
      stmt.setInt(2, currentUserId);

      stmt.execute();
      System.out.println("Comment liked successfully.");
      stmt.close();
    } catch (SQLException e) {
      if (e.getErrorCode() == 45000) { // Triggered error
        System.out.println(e.getMessage());
      } else if (e.getErrorCode() == 1062) { // Duplicate entry
        System.out.println("You have already liked this comment.");
      } else {
        System.out.println("Error liking comment:");
        System.out.println(e.getMessage());
      }
    } catch (NumberFormatException e) {
      System.out.println("Invalid CommentID format.");
    }
  }

  // Read and display post likes (only own or friends' posts)
  private void readPostLikes() {
    try {
      System.out.print("Enter PostID to view likes: ");
      int postId = Integer.parseInt(scanner.nextLine());

      // Verify access
      if (!canAccessPost(postId)) {
        System.out.println("You can only view likes on your own posts or your friends' posts.");
        return;
      }

      String query = "SELECT l.LikeID, l.UserID, u.Username, l.Timestamp " +
              "FROM post_likes l " +
              "JOIN users u ON l.UserID = u.UserID " +
              "WHERE l.PostID = ? " +
              "ORDER BY l.Timestamp ASC";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, postId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Likes on PostID: " + postId + " ---");
      while (rs.next()) {
        System.out.println("LikeID: " + rs.getInt("LikeID"));
        System.out.println("User: " + rs.getString("Username"));
        System.out.println("Timestamp: " + rs.getTimestamp("Timestamp"));
        System.out.println("---------------------------");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error reading post likes:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid PostID format.");
    }
  }

  // Read and display comment likes (only own or friends' comments)
  private void readCommentLikes() {
    try {
      System.out.print("Enter CommentID to view likes: ");
      int commentId = Integer.parseInt(scanner.nextLine());

      // Verify access
      if (!canAccessComment(commentId)) {
        System.out.println("You can only view likes on your own comments or your friends' comments.");
        return;
      }

      String query = "SELECT l.LikeID, l.UserID, u.Username, l.Timestamp " +
              "FROM comment_likes l " +
              "JOIN users u ON l.UserID = u.UserID " +
              "WHERE l.CommentID = ? " +
              "ORDER BY l.Timestamp ASC";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, commentId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Likes on CommentID: " + commentId + " ---");
      while (rs.next()) {
        System.out.println("LikeID: " + rs.getInt("LikeID"));
        System.out.println("User: " + rs.getString("Username"));
        System.out.println("Timestamp: " + rs.getTimestamp("Timestamp"));
        System.out.println("---------------------------");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error reading comment likes:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid CommentID format.");
    }
  }

  // Delete a post like
  private void deletePostLike() {
    try {
      System.out.print("Enter LikeID to delete: ");
      int likeId = Integer.parseInt(scanner.nextLine());

      // Verify ownership of the like
      if (!isLikeOwner(likeId, "post_likes")) {
        System.out.println("You can only delete your own likes.");
        return;
      }

      String sql = "{CALL DeletePostLike(?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, likeId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Post like deleted successfully.");
      } else {
        System.out.println("No post like found with the provided LikeID.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error deleting post like:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid LikeID format.");
    }
  }

  // Delete a comment like
  private void deleteCommentLike() {
    try {
      System.out.print("Enter LikeID to delete: ");
      int likeId = Integer.parseInt(scanner.nextLine());

      // Verify ownership of the like
      if (!isLikeOwner(likeId, "comment_likes")) {
        System.out.println("You can only delete your own likes.");
        return;
      }

      String sql = "{CALL DeleteCommentLike(?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, likeId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Comment like deleted successfully.");
      } else {
        System.out.println("No comment like found with the provided LikeID.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error deleting comment like:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid LikeID format.");
    }
  }

  // Check if the current user owns the like
  private boolean isLikeOwner(int likeId, String table) throws SQLException {
    String query = "SELECT UserID FROM " + table + " WHERE LikeID = ?";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, likeId);
    ResultSet rs = stmt.executeQuery();

    boolean isOwner = false;
    if (rs.next()) {
      int userId = rs.getInt("UserID");
      if (userId == currentUserId) {
        isOwner = true;
      }
    }

    rs.close();
    stmt.close();
    return isOwner;
  }

  // Comments access verification
  private boolean canAccessComment(int commentId) throws SQLException {
    String query = "SELECT c.UserID, p.UserID AS PostOwnerID " +
            "FROM comments c " +
            "JOIN posts p ON c.PostID = p.PostID " +
            "LEFT JOIN friendships f ON (f.UserID1 = ? AND f.UserID2 = p.UserID) " +
            "OR (f.UserID2 = ? AND f.UserID1 = p.UserID) " +
            "WHERE c.CommentID = ? AND (c.UserID = ? OR p.UserID = ? OR f.UserID1 IS NOT NULL OR f.UserID2 IS NOT NULL)";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, currentUserId);
    stmt.setInt(2, currentUserId);
    stmt.setInt(3, commentId);
    stmt.setInt(4, currentUserId);
    stmt.setInt(5, currentUserId);
    ResultSet rs = stmt.executeQuery();

    boolean canAccess = rs.next();

    rs.close();
    stmt.close();
    return canAccess;
  }

  // Check if the current user can access the comment
  private boolean canAccessCommentContent(int commentId) throws SQLException {
    // Implement additional logic if needed
    return canAccessComment(commentId);
  }

  // Check if the current user can access the post content
  private boolean canAccessPostContent(int postId) throws SQLException {
    return canAccessPost(postId);
  }

  // Read and display posts based on friendships
  private void readPostsForFriendship() {
    // Already implemented as readPosts()
  }


  // Check if the current user can comment on the post
  private boolean canCommentOnPost(int postId) throws SQLException {
    return canAccessPost(postId);
  }

  // Comments Menu Placeholder for further expansion

  // Messages Menu
  private void messagesMenu() {
    while (true) {
      System.out.println("\n--- Messages Menu ---");
      System.out.println("1. Send Message");
      System.out.println("2. Read Messages");
      System.out.println("3. Delete Message");
      System.out.println("4. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          sendMessage();
          break;
        case "2":
          readMessages();
          break;
        case "3":
          deleteMessage();
          break;
        case "4":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Send a message
  private void sendMessage() {
    try {
      System.out.print("Enter Receiver's Username: ");
      String receiverUsername = scanner.nextLine();

      // Get Receiver's UserID
      int receiverId = getUserIdByUsername(receiverUsername);
      if (receiverId == -1) {
        System.out.println("User not found.");
        return;
      }

      System.out.print("Enter Content: ");
      String content = scanner.nextLine();

      String sql = "{CALL SendMessage(?, ?, ?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, currentUserId);
      stmt.setInt(2, receiverId);
      stmt.setString(3, content);

      stmt.execute();
      System.out.println("Message sent successfully.");
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error sending message:");
      System.out.println(e.getMessage());
    }
  }

  // Read and display messages received by the user
  private void readMessages() {
    try {
      String query = "SELECT m.MessageID, u.Username AS Sender, m.Content, m.Timestamp " +
              "FROM messages m " +
              "JOIN users u ON m.SenderID = u.UserID " +
              "WHERE m.ReceiverID = ? " +
              "ORDER BY m.Timestamp DESC";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, currentUserId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Your Messages ---");
      while (rs.next()) {
        System.out.println("MessageID: " + rs.getInt("MessageID"));
        System.out.println("From: " + rs.getString("Sender"));
        System.out.println("Content: " + rs.getString("Content"));
        System.out.println("Timestamp: " + rs.getTimestamp("Timestamp"));
        System.out.println("---------------------------");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error reading messages:");
      System.out.println(e.getMessage());
    }
  }

  // Delete a message (only own)
  private void deleteMessage() {
    try {
      System.out.print("Enter MessageID to delete: ");
      int messageId = Integer.parseInt(scanner.nextLine());

      // Verify ownership (sender)
      if (!isMessageOwner(messageId)) {
        System.out.println("You can only delete your own messages.");
        return;
      }

      String sql = "DELETE FROM messages WHERE MessageID = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setInt(1, messageId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Message deleted successfully.");
      } else {
        System.out.println("No message found with the provided MessageID.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error deleting message:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid MessageID format.");
    }
  }

  // Check if the current user is the sender of the message
  private boolean isMessageOwner(int messageId) throws SQLException {
    String query = "SELECT SenderID FROM messages WHERE MessageID = ?";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, messageId);
    ResultSet rs = stmt.executeQuery();

    boolean isOwner = false;
    if (rs.next()) {
      int senderId = rs.getInt("SenderID");
      if (senderId == currentUserId) {
        isOwner = true;
      }
    }

    rs.close();
    stmt.close();
    return isOwner;
  }

  // Notifications Menu
  private void notificationsMenu() {
    while (true) {
      System.out.println("\n--- Notifications Menu ---");
      System.out.println("1. Read Notifications");
      System.out.println("2. Mark Notification as Read");
      System.out.println("3. Delete Notification");
      System.out.println("4. Back to Main Menu");
      System.out.print("Select an option: ");

      String choice = scanner.nextLine();

      switch (choice) {
        case "1":
          readNotifications();
          break;
        case "2":
          markNotificationAsRead();
          break;
        case "3":
          deleteNotification();
          break;
        case "4":
          return;
        default:
          System.out.println("Invalid option. Please try again.");
      }
    }
  }

  // Read and display notifications for the user
  private void readNotifications() {
    try {
      String query = "SELECT NotificationID, Content, IsRead, Timestamp " +
              "FROM notifications " +
              "WHERE UserID = ? " +
              "ORDER BY Timestamp DESC";

      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setInt(1, currentUserId);
      ResultSet rs = stmt.executeQuery();

      System.out.println("\n--- Your Notifications ---");
      while (rs.next()) {
        System.out.println("NotificationID: " + rs.getInt("NotificationID"));
        System.out.println("Content: " + rs.getString("Content"));
        System.out.println("Is Read: " + rs.getBoolean("IsRead"));
        System.out.println("Timestamp: " + rs.getTimestamp("Timestamp"));
        System.out.println("---------------------------");
      }

      rs.close();
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error reading notifications:");
      System.out.println(e.getMessage());
    }
  }

  // Mark a notification as read
  private void markNotificationAsRead() {
    try {
      System.out.print("Enter NotificationID to mark as read: ");
      int notificationId = Integer.parseInt(scanner.nextLine());

      // Verify ownership
      if (!isNotificationOwner(notificationId)) {
        System.out.println("You can only mark your own notifications as read.");
        return;
      }

      String sql = "{CALL MarkNotificationAsRead(?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, notificationId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Notification marked as read.");
      } else {
        System.out.println("No notification found with the provided NotificationID.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error marking notification as read:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid NotificationID format.");
    }
  }

  // Delete a notification (only own)
  private void deleteNotification() {
    try {
      System.out.print("Enter NotificationID to delete: ");
      int notificationId = Integer.parseInt(scanner.nextLine());

      // Verify ownership
      if (!isNotificationOwner(notificationId)) {
        System.out.println("You can only delete your own notifications.");
        return;
      }

      String sql = "{CALL DeleteNotification(?)}";
      CallableStatement stmt = conn.prepareCall(sql);
      stmt.setInt(1, notificationId);

      int rowsAffected = stmt.executeUpdate();
      if (rowsAffected > 0) {
        System.out.println("Notification deleted successfully.");
      } else {
        System.out.println("No notification found with the provided NotificationID.");
      }
      stmt.close();
    } catch (SQLException e) {
      System.out.println("Error deleting notification:");
      System.out.println(e.getMessage());
    } catch (NumberFormatException e) {
      System.out.println("Invalid NotificationID format.");
    }
  }

  // Check if the current user owns the notification
  private boolean isNotificationOwner(int notificationId) throws SQLException {
    String query = "SELECT UserID FROM notifications WHERE NotificationID = ?";
    PreparedStatement stmt = conn.prepareStatement(query);
    stmt.setInt(1, notificationId);
    ResultSet rs = stmt.executeQuery();

    boolean isOwner = false;
    if (rs.next()) {
      int userId = rs.getInt("UserID");
      if (userId == currentUserId) {
        isOwner = true;
      }
    }

    rs.close();
    stmt.close();
    return isOwner;
  }

  // Check if the current user is allowed to like the comment
  private boolean canLikeComment(int commentId) throws SQLException {
    // Implement access control if needed
    return canAccessComment(commentId);
  }

  // Check if the current user is allowed to like the post
  private boolean canLikePost(int postId) throws SQLException {
    // Implement access control if needed
    return canAccessPost(postId);
  }

  // Function to check if user can like a post/comment
  // Already handled in canAccessPost and canAccessComment

  // Check if user can comment on a post
  // Already handled in createComment

  // Update Procedures
  // Placeholder for additional update functionalities

  // Utility method to hash passwords (simple SHA-256)
  private String hashPassword(String password) {
    try {
      // In production, use a robust hashing algorithm with salt like BCrypt
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = md.digest(password.getBytes("UTF-8"));

      // Convert byte array into signum representation
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }

      return sb.toString();
    } catch (Exception e) {
      System.out.println("Error hashing password:");
      System.out.println(e.getMessage());
      return null;
    }
  }

  // Entry point of the application
  public static void main(String[] args) {
    NewSocialMediaApp app = new NewSocialMediaApp();
    if (app.connect()) {
      app.unauthenticatedMenu(); // Start with unauthenticated menu
      app.authenticatedMenu(); // After login, proceed to authenticated menu
      app.disconnect();
    }
  }
}
