#!/bin/bash

# Change to the project root directory
cd "$(dirname "$0")/.."

# Make the script executable
chmod +x ./gradlew

# Print a header
echo "Running all tests in the project..."
echo "----------------------------------------"

# Run all tests with coverage
./gradlew test jacocoTestReport

# Check if tests passed
if [ $? -eq 0 ]; then
    echo "----------------------------------------"
    echo "✅ All tests passed successfully!"
    echo "----------------------------------------"
    
    # Print coverage report location
    echo "Coverage report can be found in:"
    echo "- HTML: build/reports/jacoco/test/html/index.html"
    echo "- XML: build/reports/jacoco/test/jacocoTestReport.xml"
else
    echo "----------------------------------------"
    echo "❌ Some tests failed. Please check the output above for details."
    echo "----------------------------------------"
    exit 1
fi 