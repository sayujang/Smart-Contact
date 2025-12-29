Collecting workspace information# Smart Contact Manager (SCM)

## Overview

Smart Contact Manager (SCM) is a comprehensive web application designed to help users manage their contacts efficiently. Built with Spring Boot and modern web technologies, SCM provides a secure, cloud-based platform for storing, organizing, and sharing contact information with enhanced features like QR code sharing, real-time messaging, and OAuth2 authentication.

## Features

### Core Features

- **User Authentication & Authorization**
  - Email/password-based registration and login
  - OAuth2 integration (Google and GitHub)
  - Email verification for new accounts
  - Secure password management with BCrypt encryption

- **Contact Management**
  - Create, read, update, and delete contacts
  - Store detailed contact information (name, email, phone, address, description)
  - Add multiple social media links to contacts
  - Mark contacts as favorites
  - Search contacts by name, email, or phone number
  - Pagination support for contact lists

- **QR Code Sharing**
  - Generate unique QR codes for user profiles
  - Share contacts via QR code scanning
  - Automatic mutual contact creation

- **Real-Time Messaging**
  - WebSocket-based instant messaging
  - Message delivery and read status tracking
  - Typing indicators
  - File sharing capabilities
  - Contact availability status

- **User Profile Management**
  - Customizable profile pictures (Cloudinary integration)
  - User information editing
  - Privacy settings
  - Account deletion option

- **Security Features**
  - CSRF protection
  - JWT token support
  - Role-based access control
  - Email token verification
  - Secure session management

## Technology Stack

### Backend

- **Framework:** Spring Boot 3.x
- **Language:** Java 17+
- **Database:** 
  - Relational: JPA/Hibernate with SQL database
  - NoSQL: MongoDB for chat messages
- **Security:** Spring Security with OAuth2
- **Authentication:** JWT (JSON Web Tokens)
- **Real-Time Communication:** WebSocket with Spring Messaging

### Frontend

- **Template Engine:** Thymeleaf
- **Styling:** Tailwind CSS
- **CSS Framework:** Flowbite
- **Icons:** Font Awesome
- **Build Tool:** PostCSS

### External Services

- **Cloud Storage:** Cloudinary (for image uploads)
- **OAuth Providers:** Google and GitHub
- **Email Service:** Spring Mail

### Build & Dependency Management

- **Build Tool:** Maven
- **Package Manager:** npm (for frontend dependencies)

## Project Structure

```
src/
├── main/
│   ├── java/com/smartcontact/scm/
│   │   ├── controllers/          # REST and MVC controllers
│   │   ├── services/             # Business logic layer
│   │   ├── repositories/         # Data access layer
│   │   ├── entities/             # JPA entities and MongoDB documents
│   │   ├── config/               # Configuration classes
│   │   ├── forms/                # Form DTOs
│   │   ├── Helpers/              # Utility classes and enums
│   │   └── Validators/           # Custom validation annotations
│   └── resources/
│       ├── templates/            # Thymeleaf HTML templates
│       ├── static/               # CSS, JavaScript, and images
│       └── application.properties # Configuration file
└── test/                         # Unit and integration tests
```

## Installation & Setup

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- Node.js and npm
- MongoDB (for chat functionality)
- SQL Database (MySQL, PostgreSQL, etc.)
- Cloudinary account (for image uploads)

### Backend Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd scm
   ```

2. Configure application properties:
   - Edit application.properties
   - Set database connection details
   - Configure Cloudinary credentials
   - Set up OAuth2 credentials for Google and GitHub
   - Configure email service settings

3. Build the project:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Frontend Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Build Tailwind CSS:
   ```bash
   npm run build
   ```

3. Watch for changes (during development):
   ```bash
   npm run watch
   ```

## Configuration

### Application Properties

Key configuration variables to set in `application.properties`:

- Database connection strings
- Cloudinary API credentials
- OAuth2 client IDs and secrets
- Email service configuration
- JWT token expiration time
- MongoDB connection URI

## API Endpoints

### Authentication

- `POST /do-register` - User registration
- `POST /login` - User login
- `GET /auth/verify-email` - Email verification

### Contact Management

- `GET /user/contact` - View all contacts
- `POST /user/contact/add` - Create new contact
- `GET /user/contact/{id}` - Get contact details
- `POST /user/contact/{id}` - Update contact
- `POST /user/contact/{id}/delete` - Delete contact
- `GET /user/contact/qrcode` - Generate QR code
- `POST /user/contact/upload-qr` - Upload and scan QR code

### Messaging

- `GET /api/chat/history/{userId1}/{userId2}` - Get chat history
- `GET /api/chat/unread/{userId}` - Get unread message count
- `GET /api/chat/unknown/{userId}` - Get users not in contacts
- `POST /api/chat/upload` - Upload file in chat

### User Management

- `GET /user/profile` - View user profile
- `POST /user/settings` - Update profile settings
- `POST /user/settings/change-password` - Change password
- `POST /user/delete-account` - Delete user account

## WebSocket Events

### Message Mapping

- `/app/chat.register` - Register user for messaging
- `/app/chat.send` - Send message to contact
- `/app/chat.typing` - Send typing indicator
- `/app/chat.read` - Mark messages as read

### Subscription Topics

- `/queue/messages` - Receive messages
- `/queue/read/{userId}` - Receive read receipts
- `/queue/typing/{userId}` - Receive typing indicators
- `/queue/status` - User status updates

## Database Schema

### Key Entities

- **User** - User account information with OAuth provider details
- **Contact** - Contact information linked to user
- **SocialLink** - Social media profiles for contacts
- **ChatMessage** - Real-time messages between users (MongoDB)
- **UserStatus** - User online/offline status tracking

## Security Considerations

- All passwords are encrypted using BCrypt
- CSRF tokens are required for state-changing requests
- OAuth2 integration handles secure third-party authentication
- Email verification prevents account abuse
- JWT tokens with expiration for websocket while sessions for user login

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify database is running
   - Check connection string in application.properties
   - Ensure credentials are correct

2. **Cloudinary Upload Fails**
   - Verify Cloudinary credentials
   - Check API key and secret
   - Ensure image file size is within limits

3. **OAuth2 Login Not Working**
   - Verify redirect URIs are configured correctly
   - Check client ID and secret
   - Ensure allowed redirect hosts match

4. **WebSocket Connection Issues**
   - Check WebSocket configuration
   - Verify firewall allows WebSocket connections
   - Check browser console for connection errors

## Performance Optimization

- Implement pagination for large datasets
- Use lazy loading for relationships
- Cache frequently accessed data
- Compress static assets
- Optimize database queries with proper indexing

## Future Enhancements

- Contact groups and categories
- Advanced search and filtering
- Contact import/export functionality
- Scheduled messaging
- Voice and video calling
- Contact backup and synchronization
- Multi-language support
