import psycopg2

def check_postgres():
    try:
        conn = psycopg2.connect(
            dbname="enterprise_ai",
            user="postgres",
            password="2005",
            host="localhost",
            port="5432"
        )
        cur = conn.cursor()
        
        # Check if column exists and is populated
        cur.execute("SELECT id, document_id, chunk_index, length(content) as content_len, search_vector is not null as has_vector FROM document_chunks LIMIT 5;")
        rows = cur.fetchall()
        print("--- Postgres document_chunks sample ---")
        for r in rows:
            print(f"ID: {r[0]}, DocID: {r[1]}, Index: {r[2]}, Len: {r[3]}, HasVector: {r[4]}")
            
        # Try a sample full text search query
        cur.execute("""
            SELECT id, ts_rank(search_vector, to_tsquery('english', 'ceo')) as score
            FROM document_chunks
            WHERE search_vector @@ to_tsquery('english', 'ceo')
            ORDER BY score DESC LIMIT 3;
        """)
        fts_rows = cur.fetchall()
        print("\n--- Postgres FTS query for 'ceo' ---")
        for r in fts_rows:
            print(f"ID: {r[0]}, Score: {r[1]}")
            
        cur.close()
        conn.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_postgres()
