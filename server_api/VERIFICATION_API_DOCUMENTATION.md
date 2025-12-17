# WorldMates Messenger - Verification API Documentation

## Overview
This API provides endpoints for user registration and login with SMS/Email verification.

## Base URL
```
https://your-domain.com/xhr/
```

## Authentication
Most endpoints require an access token passed in the `access_token` parameter.

---

## 1. Register with Verification

**Endpoint:** `POST /xhr/index.php?f=register_with_verification`

**Description:** Register a new user and send verification code via SMS or Email.

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| username | string | Yes | Username (5-32 characters, alphanumeric) |
| password | string | Yes | Password (minimum 6 characters) |
| confirm_password | string | Yes | Password confirmation |
| verification_type | string | Yes | "email" or "phone" |
| email | string | Conditional | Required if verification_type is "email" |
| phone_number | string | Conditional | Required if verification_type is "phone" (format: +380...) |
| gender | string | No | "male" or "female" (default: "male") |

### Response (Success)

```json
{
  "api_status": 200,
  "message": "Registration successful! Verification code sent to your email",
  "user_id": 123,
  "username": "john_doe",
  "verification_type": "email",
  "contact_info": "john@example.com",
  "code_length": 6,
  "expires_in": 600
}
```

### Response (Error)

```json
{
  "api_status": 400,
  "errors": "Username already exists"
}
```

### Example Request (Email)

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=register_with_verification' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'username=john_doe' \
  -d 'password=securepass123' \
  -d 'confirm_password=securepass123' \
  -d 'verification_type=email' \
  -d 'email=john@example.com' \
  -d 'gender=male'
```

### Example Request (Phone)

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=register_with_verification' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'username=john_doe' \
  -d 'password=securepass123' \
  -d 'confirm_password=securepass123' \
  -d 'verification_type=phone' \
  -d 'phone_number=+380930253941' \
  -d 'gender=male'
```

---

## 2. Verify Code

**Endpoint:** `POST /xhr/index.php?f=verify_code`

**Description:** Verify the code sent via SMS or Email and activate the account.

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| verification_type | string | Yes | "email" or "phone" |
| contact_info | string | Yes | Email address or phone number |
| code | string | Yes | 6-digit verification code |
| username | string | No | Username (if known) |
| user_id | integer | No | User ID (if known) |

### Response (Success)

```json
{
  "api_status": 200,
  "message": "Email verified successfully",
  "user_id": 123,
  "access_token": "abc123def456...",
  "timezone": "UTC"
}
```

### Response (Error)

```json
{
  "api_status": 400,
  "errors": "Wrong confirmation code"
}
```

### Example Request

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=verify_code' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'verification_type=email' \
  -d 'contact_info=john@example.com' \
  -d 'code=123456' \
  -d 'username=john_doe'
```

---

## 3. Send Verification Code

**Endpoint:** `POST /xhr/index.php?f=send_verification_code`

**Description:** Send a verification code to an existing user.

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| verification_type | string | Yes | "email" or "phone" |
| contact_info | string | Yes | Email address or phone number |
| username | string | No | Username (if known) |
| user_id | integer | No | User ID (if known) |

### Response (Success)

```json
{
  "status": 200,
  "message": "Verification code sent to your email",
  "code_length": 6,
  "expires_in": 600
}
```

### Example Request

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=send_verification_code' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'verification_type=email' \
  -d 'contact_info=john@example.com' \
  -d 'username=john_doe'
```

---

## 4. Resend Verification Code

**Endpoint:** `POST /xhr/index.php?f=resend_verification_code`

**Description:** Resend a new verification code (in case the previous one expired or was not received).

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| verification_type | string | Yes | "email" or "phone" |
| contact_info | string | Yes | Email address or phone number |
| username | string | No | Username (if known) |
| user_id | integer | No | User ID (if known) |

### Response (Success)

```json
{
  "api_status": 200,
  "message": "Verification code resent to your email",
  "code_length": 6,
  "expires_in": 600
}
```

### Example Request

```bash
curl -X POST 'https://your-domain.com/xhr/index.php?f=resend_verification_code' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'verification_type=phone' \
  -d 'contact_info=+380930253941' \
  -d 'username=john_doe'
```

---

## Error Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 400 | Bad Request / Validation Error |
| 404 | Not Found |
| 500 | Internal Server Error |

## Common Errors

- **"Username already exists"** - The username is already taken
- **"Email already exists"** - The email is already registered
- **"Phone already used"** - The phone number is already registered
- **"Wrong confirmation code"** - The verification code is incorrect
- **"Wrong phone number"** - Invalid phone number format
- **"User not found"** - No user found with the provided credentials

---

## Flow Diagrams

### Registration Flow (Email)

```
1. User submits registration form
   ↓
2. POST /register_with_verification (verification_type=email)
   ↓
3. User receives email with 6-digit code
   ↓
4. User enters code in app
   ↓
5. POST /verify_code
   ↓
6. Account activated + Auto-login (access_token returned)
```

### Registration Flow (Phone)

```
1. User submits registration form
   ↓
2. POST /register_with_verification (verification_type=phone)
   ↓
3. User receives SMS with 6-digit code
   ↓
4. User enters code in app
   ↓
5. POST /verify_code
   ↓
6. Account activated + Auto-login (access_token returned)
```

### Resend Code Flow

```
1. User clicks "Resend Code"
   ↓
2. POST /resend_verification_code
   ↓
3. New code sent via SMS/Email
   ↓
4. User enters new code
   ↓
5. POST /verify_code
```

---

## SMS Provider Configuration

The API supports multiple SMS providers:

- **Twilio** - Configure in admin panel
- **Infobip** - Configure in admin panel

### Required Configuration (Twilio)

```
sms_provider: "twilio"
sms_twilio_username: "your_account_sid"
sms_twilio_password: "your_auth_token"
sms_t_phone_number: "+1234567890"
```

### Required Configuration (Infobip)

```
sms_provider: "infobip"
infobip_api_key: "your_api_key"
infobip_base_url: "https://api.infobip.com"
```

---

## Testing

### Test with Postman

1. Import the endpoints into Postman
2. Set up environment variables for your domain
3. Test registration flow:
   - Register → Verify → Login

### Test with cURL

See examples above for each endpoint.

---

## Notes

- Verification codes expire after 10 minutes (600 seconds)
- Codes are 6 digits long
- Phone numbers should be in international format (+380...)
- Email addresses must be valid
- Usernames must be 5-32 characters, alphanumeric only
- Passwords must be at least 6 characters

---

## Support

For issues or questions, contact: support@worldmates.club
