# Offline ML Foundation

This directory contains the offline Machine Learning pipeline for Gathr.

## Structure

- `etl/`: Scripts for Extract, Transform, Load operations.
- `training/`: Scripts for model training and evaluation.
- `data/`: Local storage for extracted CSVs (gitignored).
- `models/`: Local storage for trained models (gitignored).

## Setup

1. **Create a virtual environment:**
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

2. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Environment Variables:**
   Create a `.env` file in this directory with your database credentials:
   ```
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=gathr
   DB_USER=postgres
   DB_PASS=postgres
   ```

## Workflow

1. **Extract Data:**
   Pull the latest event logs from the database.
   ```bash
   python etl/extract.py
   ```
   This will create `data/event_logs.csv`.

2. **Train Model:**
   Train the XGBoost model on the extracted data.
   ```bash
   python training/train.py
   ```
   This will save the model to `models/xgboost_v1.json`.

## Future Work

- Add feature store logic.
- Implement model versioning (MLflow).
- Automate pipeline with Airflow or similar.
