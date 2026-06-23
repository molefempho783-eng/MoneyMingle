[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/kSi58dQ9)

# Money Mingle

**Module:** PROG7313 Programming 3C
**Group members:** ST10434242, ST10446408, ST10317078, ST10443277, ST10294763
**Part:** 3 - Final PoE

## Demo Video

[Click here to watch the demo video](https://youtu.be/-9P8GcGPXVE)

---

## Overview

Money Mingle is a personal finance tracking application built in Kotlin using MVVM architecture. Users register and log in with an email address and password via Firebase Authentication. All data is stored in Firebase Firestore and syncs in real time across sessions.

---

## Core Features

- Register and log in with email and password (Firebase Authentication)
- Create, edit, and delete expense categories with monthly budget goals (min and max)
- Log expenses with a date, description, amount, category, and optional receipt photo
- View all expenses over a user-selected date range
- View a spending graph showing amount spent per category over a selectable period, with min/max goal lines displayed
- View a budget overview showing how well spending stayed within goals over the past month
- All data stored in Firebase Firestore (online, syncs across devices)

---

## Custom Feature 1 - Wishlist with Progress Tracking

### What it does

The Wishlist allows users to set personal savings goals and track their progress toward each one. A goal has a name, a target amount (in Rands), and a target date. Users log contributions toward a goal over time. The app tracks how much has been saved and displays progress visually.

Each goal card on the list screen shows:
- The goal name and target amount
- How much has been saved so far (e.g. R 350 / R 1 000)
- A percentage and colour-coded progress bar (amber while in progress, bright green when complete)
- Time remaining until the target date (shown as days, weeks, months, or years)
- The target date formatted as a readable date (e.g. Target: 14 Aug 2026)
- A **Completed** badge once the saved amount reaches or exceeds the target
- An **Add Contribution** button (hidden once the goal is complete)

### How to use it

1. Open the app and log in.
2. On the Dashboard, tap the **Wishlist** quick-link card.
3. The Wishlist screen opens showing all existing goals. Tap the **+** button at the bottom to create a new goal.
4. Fill in the goal name, target amount, and target date, then tap **Save**.
5. The new goal appears in the list. Tap **Add Contribution** on a goal card to log money saved toward it. Enter the amount and confirm.
6. The saved amount and progress bar update immediately.
7. To edit or delete a goal, tap the goal card. The edit form loads with existing values. A **Delete** button is available at the bottom.

---

## Custom Feature 2 - Subscription Manager

### What it does

The Subscription Manager lets users track all their recurring payments in one place. Each subscription has a name, a billing amount, a billing cycle (weekly, monthly, or yearly), and a start date.

The app normalises every subscription to a **monthly equivalent cost** and displays the running total at the top of the screen so users can see exactly how much they are committing to per month across all subscriptions:

| Billing cycle | Monthly equivalent formula |
|---|---|
| Weekly | amount x 52 / 12 |
| Monthly | amount (used as-is) |
| Yearly | amount / 12 |

Each subscription card shows:
- Subscription name
- Amount and billing cycle label (e.g. R 169,00 - Monthly)
- The **next renewal date**, calculated by stepping forward from the start date by the billing interval until a date in the future is reached
- A colour-coded card border: **purple** for monthly, **amber** for weekly, **green** for yearly

The hero card at the top of the list shows:
- Total monthly spend across all subscriptions
- Total number of active subscriptions

### How to use it

1. Open the app and log in.
2. On the Dashboard, tap the **Subscriptions** quick-link card.
3. The Subscription Manager screen opens. Tap the **+** floating action button to add a subscription.
4. Fill in the subscription name, amount, billing cycle (tap the dropdown to choose Weekly / Monthly / Yearly), and start date, then tap **Save**.
5. The subscription appears in the list. The monthly total in the hero card updates automatically.
6. To edit or delete a subscription, tap its card. The edit form opens with existing values loaded. A **Delete** button is available at the bottom.

---

## Additional Features (Part 3)

The following features were added for Part 3 in addition to the two custom features above:

- **Spending Graph** - A bar/line chart showing total amount spent per category over a user-selected period. Min and max budget goal lines are overlaid on the chart so users can see at a glance whether they stayed within their targets.
- **Budget Overview** - A visual summary of how well the user stayed between their minimum and maximum spending goals over the past month. Each category shows spent vs goal with a colour-coded indicator.
- **Gamification** - A supporting add-on that rewards users for consistent financial habits. Tracks a daily logging streak, awards badges for milestones (e.g. 3-day streak, 7-day streak, first expense, receipt uploads), and includes time-based challenges (weekly and monthly receipt challenges, Early Bird monthly bonus). Accessible via the **Budget Challenges** screen.
- **Firebase Authentication** - Email and password login replacing the old local username/hash system.
- **Firebase Firestore** - All data (expenses, categories, budget goals, subscriptions, savings goals, gamification stats) stored in the cloud and accessible from any device.
