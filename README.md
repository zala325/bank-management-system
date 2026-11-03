# Bank Management System

A console app to manage bank records and accounts.

**Tools:** Java 17, MongoDB (NoSQL), Maven

## Features
- Create, view, list and delete accounts
- Deposit, withdraw and transfer money
- Transaction history (last 20)
- Balance can never go below zero

## How to run

1. Install **Java 17+**, **Maven** and **MongoDB Community Server**.
2. Make sure MongoDB is running on `localhost:27017`.
3. In this folder, run:

```
mvn compile exec:java
```

The database `bank_db` and its collections are created automatically.

## Database design (NoSQL)

**accounts** collection
```json
{
  "accountNumber": "AC123456",
  "name": "Sarang Zala",
  "accountType": "Savings",
  "balance": 5000.0,
  "createdAt": "2026-11-03T10:00:00Z"
}
```

**transactions** collection
```json
{
  "accountNumber": "AC123456",
  "type": "DEPOSIT",
  "amount": 1000.0,
  "balanceAfter": 5000.0,
  "note": "Cash deposit",
  "date": "2026-11-03T10:05:00Z"
}
```

## Useful MongoDB shell commands (mongosh)

```js
use bank_db
db.accounts.find().pretty()
db.transactions.find({ accountNumber: "AC123456" })
db.accounts.countDocuments()
```
