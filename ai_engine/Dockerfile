# AI Engine Dockerfile (Python 3.11)
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt ./
# Install CPU-only PyTorch first so ultralytics doesn't pull GPU wheels
RUN pip install --no-cache-dir torch==2.4.1 --index-url https://download.pytorch.org/whl/cpu
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
