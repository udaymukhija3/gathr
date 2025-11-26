import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, roc_auc_score
import os
import json

def load_data(filepath="data/event_logs.csv"):
    """
    Loads data from CSV.
    """
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        return None
    return pd.read_csv(filepath)

def preprocess_data(df):
    """
    Preprocesses the raw event logs into features and labels.
    Target: 1 if event_type is 'PLAN_JOINED' or 'PLAN_CONFIRMED', 0 otherwise (e.g. 'PLAN_VIEWED' without join).
    
    NOTE: This is a simplified V1 logic. Real logic needs to group by (user, activity) session.
    """
    print("Preprocessing data...")
    
    # Filter for relevant events
    relevant_events = ['PLAN_VIEWED', 'PLAN_JOINED', 'PLAN_CONFIRMED']
    df = df[df['event_type'].isin(relevant_events)].copy()
    
    # Simple Label Creation: 
    # For this V1, let's assume we are predicting if a VIEW leads to a JOIN/CONFIRM.
    # In reality, we'd need to construct negative samples properly.
    # Here, we will just mock some features for demonstration as we might not have enough data yet.
    
    # Mocking features for the skeleton
    # In production, extract these from 'metadata' JSON column
    df['hour_of_day'] = pd.to_datetime(df['created_at']).dt.hour
    df['day_of_week'] = pd.to_datetime(df['created_at']).dt.dayofweek
    
    # Mock target: 1 if JOINED/CONFIRMED, 0 if VIEWED
    df['target'] = df['event_type'].apply(lambda x: 1 if x in ['PLAN_JOINED', 'PLAN_CONFIRMED'] else 0)
    
    # Select features
    features = ['hour_of_day', 'day_of_week']
    X = df[features]
    y = df['target']
    
    return X, y

def train_model(X, y):
    """
    Trains an XGBoost classifier.
    """
    print("Training model...")
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    model = xgb.XGBClassifier(
        objective='binary:logistic',
        eval_metric='logloss',
        use_label_encoder=False
    )
    
    model.fit(X_train, y_train)
    
    # Evaluate
    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]
    
    acc = accuracy_score(y_test, y_pred)
    try:
        auc = roc_auc_score(y_test, y_prob)
    except:
        auc = 0.5 # Handle case with only one class
        
    print(f"Model Trained. Accuracy: {acc:.4f}, AUC: {auc:.4f}")
    
    return model

def save_model(model, output_path="models/xgboost_v1.json"):
    """
    Saves the trained model.
    """
    os.makedirs("models", exist_ok=True)
    model.save_model(output_path)
    print(f"Model saved to {output_path}")

if __name__ == "__main__":
    df = load_data()
    if df is not None and not df.empty:
        X, y = preprocess_data(df)
        if not X.empty:
            model = train_model(X, y)
            save_model(model)
        else:
            print("No data to train on.")
    else:
        print("Data extraction needed. Run etl/extract.py first.")
