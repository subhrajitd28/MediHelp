FROM python:3.12-slim

# Tesseract for the Layer 1 OCR step in describe_image()
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    libgl1 \
    libglib2.0-0 \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Source — PDFs in data/ are not needed at runtime (Pinecone holds the embeddings).
# To rebuild the index, run store_index.py from a dev box, not from the image.
COPY app.py db.py eureka_register.py ./
COPY src/ ./src/
COPY templates/ ./templates/
COPY static/ ./static/

EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
  CMD curl -f http://localhost:8086/actuator/health || exit 1

CMD ["python", "app.py"]
