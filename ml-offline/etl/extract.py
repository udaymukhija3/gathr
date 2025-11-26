import os
import pandas as pd
from sqlalchemy import create_engine
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Database connection details (defaults or from env)
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "gathr")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "postgres")

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

def extract_data():
    """
    Connects to the database and extracts event logs into a DataFrame.
    Saves the data to a CSV file for training.
    """
    print(f"Connecting to database: {DATABASE_URL}")
    try:
        engine = create_engine(DATABASE_URL)
        
        # Query to fetch relevant data for training
        # We want to join event_logs with activity details if possible, 
        # or just fetch event_logs and process them.
        # For V1, let's fetch raw event logs.
        query = """
        SELECT 
            el.id,
            el.user_id,
            el.activity_id,
            el.event_type,
            el.metadata,
            el.created_at
        FROM 
            event_logs el
        WHERE 
            el.created_at >= NOW() - INTERVAL '30 days'
        """
        
        print("Executing query...")
        df = pd.read_sql(query, engine)
        
        output_path = "data/event_logs.csv"
        os.makedirs("data", exist_ok=True)
        df.to_csv(output_path, index=False)
        
        print(f"Extraction complete. {len(df)} rows saved to {output_path}")
        return df
        
    except Exception as e:
        print(f"Error extracting data: {e}")
        return None

if __name__ == "__main__":
    extract_data()
