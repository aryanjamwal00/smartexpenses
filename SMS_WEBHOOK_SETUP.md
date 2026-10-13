# SMS Webhook Setup Guide

This guide explains how to set up automatic SMS detection for your expense tracker using SMS forwarding services.

## How It Works

1. **SMS Forwarding Service**: Forwards incoming SMS to your webhook URL
2. **Webhook Endpoint**: Your app receives SMS and automatically categorizes expenses
3. **User Association**: SMS is associated with user by phone number

## Step 1: Register with Phone Number

1. Open the app
2. Sign up with your phone number (e.g., +919876543210)
3. This phone number will be used to identify your account

## Step 2: Choose SMS Forwarding Service

### Option A: Twilio (Recommended)

**Free Tier**: $15 credit when you sign up

1. **Create Twilio Account**
   - Go to https://www.twilio.com
   - Sign up for free account
   - Get $15 free credit

2. **Purchase a Phone Number**
   - Go to "Buy a Number"
   - Select your country (India)
   - Choose a local or toll-free number
   - Cost: ~$1/month (can use free credit)

3. **Configure SMS Forwarding**
   - Go to "Messaging" → "Settings" → "Messaging Services"
   - Create a new messaging service
   - Add your phone number
   - Set "When a message comes in" to your webhook URL:
     ```
     https://your-app-url.onrender.com/api/expenses/sms-webhook
     ```

4. **Set Webhook Secret (Optional but Recommended)**
   - Generate a random secret in Render: `WEBHOOK_SECRET=your-random-secret`
   - Add header `X-Webhook-Secret: your-random-secret` in Twilio webhook config

### Option B: Plivo

**Free Tier**: Free trial credits available

1. **Create Plivo Account**
   - Go to https://www.plivo.com
   - Sign up for free trial

2. **Purchase a Phone Number**
   - Buy a phone number in your country
   - Cost: ~$1/month

3. **Configure Webhook**
   - Go to "SMS" → "Application"
   - Create new application
   - Set "Message URL" to:
     ```
     https://your-app-url.onrender.com/api/expenses/sms-webhook
     ```
   - Assign the application to your phone number

### Option C: Gupshup (India-focused)

**Free Tier**: Free trial available

1. **Create Gupshup Account**
   - Go to https://www.gupshup.io
   - Sign up for free trial

2. **Configure Webhook**
   - Go to "API" → "Webhooks"
   - Set webhook URL to:
     ```
     https://your-app-url.onrender.com/api/expenses/sms-webhook
     ```

## Step 3: Test the Webhook

After setting up the SMS service:

1. **Send a test SMS** to your purchased number:
   ```
   Rs 450 debited from account to swiggy@ybl for order #12345
   ```

2. **Check your app dashboard** - the expense should appear automatically

3. **Verify categorization** - Swiggy should be categorized under "Food & Dining"

## Step 4: Environment Variables (Optional)

For security, set these in Render:

```bash
WEBHOOK_SECRET=your-random-secret-here
```

Then configure your SMS service to send this header:
```
X-Webhook-Secret: your-random-secret-here
```

## Supported SMS Formats

The webhook automatically handles multiple SMS service formats:

- **Twilio**: `{ "Body": "SMS text", "From": "+919876543210" }`
- **Plivo**: `{ "text": "SMS text", "from": "919876543210" }`
- **Generic**: `{ "message": "SMS text", "sender": "919876543210" }`

## Automatic Processing

When an SMS is received:

1. **Extract phone number** from webhook payload
2. **Find user** by phone number
3. **Parse SMS** to extract merchant, amount, type
4. **Categorize** automatically based on merchant
5. **Save transaction** to user's account
6. **Return success** to prevent webhook retries

## Important Notes

- **Phone Number Must Match**: The phone number in your account must match the number in the SMS (cleaned: digits only)
- **Duplicate Detection**: Same SMS won't be processed twice
- **Security**: Use webhook secret to prevent unauthorized access
- **Cost**: SMS forwarding services charge per SMS (check pricing)

## Cost Comparison

| Service | Monthly Cost | Per SMS | Free Trial |
|---------|--------------|---------|------------|
| Twilio  | ~$1 (number) | ~$0.0075 | $15 credit |
| Plivo   | ~$1 (number) | ~$0.005 | Free trial |
| Gupshup | ~$1 (number) | ~$0.008 | Free trial |

## Troubleshooting

### SMS not appearing in app

1. Check webhook URL is correct
2. Verify phone number matches your account
3. Check SMS service dashboard for webhook delivery logs
4. Ensure webhook returns 200 OK

### User not found error

1. Register your phone number in the app
2. Make sure phone number format matches (digits only)
3. Update your profile with correct phone number

### Duplicate SMS

This is normal - the app prevents duplicate transactions automatically.

## Alternative: Bank Statement Import

If SMS forwarding is too expensive, use bank statement import:
- Download monthly bank statement (PDF/CSV)
- Upload to the app
- All transactions imported at once
- Free and works with current setup
