# SmartExpense - Docker + Cloud Deployment Guide

## Local Development with Docker

### Prerequisites
- Docker Desktop installed and running
- Git

### Running Locally with Docker Compose

1. **Start Docker Desktop** on your computer

2. **Build and run with PostgreSQL:**
   ```bash
   docker-compose up --build
   ```

3. **Access the application:**
   - Web App: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console (if using H2)
   - PostgreSQL: localhost:5432 (if using docker-compose)

4. **Stop the application:**
   ```bash
   docker-compose down
   ```

### Running Locally without Docker (H2 Database)

```bash
./mvnw spring-boot:run
```

Access at: http://localhost:8080

---

## Cloud Deployment to Render.com

### Step 1: Create Render Account
1. Go to https://render.com
2. Sign up for a free account
3. Connect your GitHub account

### Step 2: Push Code to GitHub

```bash
git init
git add .
git commit -m "Initial commit with Docker and Render config"
git branch -M main
# Create a new repository on GitHub first, then:
git remote add origin https://github.com/YOUR_USERNAME/smartexpense.git
git push -u origin main
```

### Step 3: Deploy to Render

1. **Log in to Render** and click "New +"
2. **Select "Web Service"**
3. **Connect your GitHub repository** (smartexpense)
4. **Configure the service:**
   - Name: smartexpense
   - Runtime: Docker
   - Build Command: (leave empty - Render will use Dockerfile)
   - Start Command: (leave empty - Render will use Dockerfile ENTRYPOINT)
   - Plan: Free

5. **Add PostgreSQL Database:**
   - After creating the web service, click "New +"
   - Select "PostgreSQL"
   - Name: smartexpense-db
   - Database: smartexpense
   - User: smartexpense
   - Plan: Free

6. **Link Database to Web Service:**
   - Go to your web service settings
   - Scroll to "Environment Variables"
   - Render automatically provides `DATABASE_URL` - no manual configuration needed

7. **Deploy!**
   - Render will automatically build and deploy your Docker container
   - Your app will be available at: `https://smartexpense.onrender.com`

### Step 4: Install on Mobile

1. **Open your phone's browser** and go to your Render URL
2. **Add to Home Screen:**
   - Android: Chrome menu → "Add to Home screen"
   - iOS: Share button → "Add to Home Screen"
3. **The app will install** as a PWA (Progressive Web App)
4. **It will work from anywhere** - no WiFi needed!

---

## Environment Variables

### Local Development (docker-compose)
- `DATABASE_URL`: Automatically set by docker-compose
- PostgreSQL credentials in docker-compose.yml

### Production (Render)
- `DATABASE_URL`: Automatically provided by Render
- `PORT`: Automatically set by Render (usually 8080)

---

## Database Notes

### H2 (Local, no Docker)
- In-memory database
- Data is lost when app stops
- Good for testing only

### PostgreSQL (Local with Docker Compose)
- Persistent data in Docker volume
- Data survives container restarts
- Good for local development

### PostgreSQL (Render Cloud)
- Persistent cloud database
- Data survives deployments
- Free tier: 90 days (then $7/month or export data)

---

## Troubleshooting

### Docker Build Fails
- Ensure Docker Desktop is running
- Check for sufficient disk space
- Try: `docker system prune -a` to clean up

### Render Deployment Fails
- Check Render dashboard for build logs
- Ensure Dockerfile is in repository root
- Verify render.yaml is in repository root

### Database Connection Issues
- Check DATABASE_URL environment variable
- Verify PostgreSQL is running (local: `docker ps`, Render: dashboard)
- Check DatabaseConfig.java for URL parsing logic

---

## Mobile App Features

Your app is already a PWA (Progressive Web App):
- ✅ Installable on mobile (no APK needed)
- ✅ Works offline (with cached data)
- ✅ Auto-updates when you deploy new version
- ✅ Responsive design for mobile screens

## New Features

### Merchant Categorization
The app now automatically categorizes expenses based on merchant names:

**Categories:**
- Food & Dining: Swiggy, Zomato, Dominos, KFC, McDonald's, etc.
- Groceries: Blinkit, Zepto, Big Basket, Dunzo, etc.
- Shopping: Amazon, Flipkart, Myntra, AJIO, Nykaa, etc.
- Transport: Uber, Ola, Rapido, Namma Yatri, IRCTC, etc.
- Entertainment: Netflix, Amazon Prime, Spotify, Hotstar, etc.
- Utilities: Paytm, PhonePe, GPay, Airtel, Jio, etc.
- Health & Wellness: 1mg, Netmeds, Pharmeasy, etc.
- Education: Coursera, Udemy, Byju's, etc.

### Merchant Breakdown
View detailed merchant-wise spending in the "Merchant Breakdown" section, showing:
- Merchant name
- Category
- Total amount spent
- Sorted by highest spending

### Refund Handling
- Refunds (CREDIT transactions) are automatically detected
- Total refund amount shown with green badge
- Detailed refund list with merchant, amount, and date
- Historical reports show net spend (after subtracting refunds)

### Add Custom Merchant Rules
Use the API to add custom merchant categorization:
```bash
POST /api/expenses/merchant-rule
{
  "keyword": "NEW_MERCHANT",
  "category": "Shopping"
}
```

### Manual Entry Portal
Add expenses manually without bank statements or SMS:
- Enter merchant name (e.g., "Swiggy", "Amazon")
- Enter amount
- Select category (or auto-detect based on merchant)
- Choose transaction type (Expense/Debit or Refund/Credit)
- Click "Add Entry"

**API Example:**
```bash
POST /api/expenses/manual-entry
{
  "merchant": "Swiggy",
  "amount": "450.00",
  "category": "Food & Dining",
  "type": "DEBIT"
}
```

---

## Next Steps

1. **Test locally:** `docker-compose up --build`
2. **Push to GitHub** and deploy to Render
3. **Install on mobile** from the Render URL
4. **Use from anywhere** - no laptop or WiFi needed!
