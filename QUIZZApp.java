public class UserService {
    private static final String INSERT_USER_QUERY = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
    
    public boolean registerUser(String username, String password, String email) {
        String hashedPassword = hashPassword(password);
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_USER_QUERY)) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String hashPassword(String password) {
        // Use a secure hashing algorithm like bcrypt, PBKDF2, or Argon2
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}public class LoginService {
    private static final String GET_USER_QUERY = "SELECT * FROM users WHERE username = ?";

    public boolean authenticateUser(String username, String password) {
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(GET_USER_QUERY)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return BCrypt.checkpw(password, hashedPassword); // Checking hashed password
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT,
    question_text TEXT NOT NULL,
    type ENUM('MCQ', 'TRUE_FALSE') NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE options (
    id INT AUTO_INCREMENT PRIMARY KEY,
    question_id INT,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

CREATE TABLE user_scores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    score INT,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);public class QuizService {
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String query = "SELECT name FROM categories";
        try (Connection connection = Database.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }
    
    public List<Question> getQuestionsByCategory(String category) {
        List<Question> questions = new ArrayList<>();
        String query = "SELECT * FROM questions WHERE category_id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Question question = new Question();
                question.setId(rs.getInt("id"));
                question.setText(rs.getString("question_text"));
                question.setType(rs.getString("type"));
                questions.add(question);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questions;
    }
}public class TimerTask implements Runnable {
    private int timeLimitInSeconds;
    private QuizSession quizSession;
    
    public TimerTask(int timeLimitInSeconds, QuizSession quizSession) {
        this.timeLimitInSeconds = timeLimitInSeconds;
        this.quizSession = quizSession;
    }

    @Override
    public void run() {
        try {
            while (timeLimitInSeconds > 0) {
                Thread.sleep(1000);
                timeLimitInSeconds--;
                quizSession.updateTimer(timeLimitInSeconds);
            }
            quizSession.finishQuiz();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}public class ScoreService {
    public void saveScore(int userId, int score) {
        String query = "INSERT INTO user_scores (user_id, score) VALUES (?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, score);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}public class AdminPanelService {
    private static final String INSERT_QUESTION_QUERY = "INSERT INTO questions (category_id, question_text, type) VALUES (?, ?, ?)";
    private static final String INSERT_OPTION_QUERY = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";

    public void createQuestion(int categoryId, String questionText, String questionType) {
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_QUESTION_QUERY)) {
            stmt.setInt(1, categoryId);
            stmt.setString(2, questionText);
            stmt.setString(3, questionType);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createOption(int questionId, String optionText, boolean isCorrect) {
        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(INSERT_OPTION_QUERY)) {
            stmt.setInt(1, questionId);
            stmt.setString(2, optionText);
            stmt.setBoolean(3, isCorrect);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}