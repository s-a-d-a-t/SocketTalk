# SocketTalk User Roles and Test Accounts

## User Roles

The system supports three distinct user roles:

### 1. STUDENT
- Can send private messages to other students
- Can create and join group chats for assignments or collaboration
- Can view other users' online/offline status
- Receives message notifications

**Test Account:**
- ID: `3369` or `3774`
- Username: `sadat` or `wosen`
- Password: `sadat123` or `wosen123`

### 2. TEACHER
- Can register and log in like students
- Can create classroom groups
- Can broadcast announcements to all students in a group
- Can send private messages
- Has same messaging capabilities as students

**Test Account:**
- ID: `2222`
- Username: `mr`
- Password: `mr123`

### 3. ADMIN
- Has access to the Admin Dashboard
- Can view system statistics (total users, online users, total messages)
- Can monitor all registered users and their online status
- Can view user details (ID, Name, Role, Status)
- Does not participate in regular messaging

**Test Account:**
- ID: `1111`
- Username: `admin`
- Password: `admin123`

## Key Differences

| Feature | STUDENT | TEACHER | ADMIN |
|---------|---------|---------|-------|
| Private Messaging | ✅ | ✅ | ❌ |
| Group Chats | ✅ | ✅ | ❌ |
| Create Classroom Groups | ❌ | ✅ | ❌ |
| Broadcast Announcements | ❌ | ✅ | ❌ |
| Admin Dashboard Access | ❌ | ❌ | ✅ |
| View System Statistics | ❌ | ❌ | ✅ |
| Monitor All Users | ❌ | ❌ | ✅ |

## How to Test

### Testing as Student/Teacher:
1. Login with student or teacher credentials
2. View user list with online/offline indicators
3. Send messages and see notification badges
4. Teachers can create groups and broadcast

### Testing as Admin:
1. Login with admin credentials (ID: `1111`, Password: `admin123`)
2. Click "Admin Panel" button in main view
3. View statistics cards showing:
   - Total registered users
   - Currently online users
   - Total messages sent
4. View user table with all registered users
5. Click "Refresh Data" to update statistics
6. Click "Back to Chat" to return to main view
