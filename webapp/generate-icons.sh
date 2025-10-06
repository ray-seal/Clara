#!/bin/bash

# Icon generation script for Clara PWA
# This script generates all required icon sizes from the base SVG

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "ImageMagick not found. Installing..."
    # Try to install ImageMagick based on the system
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y imagemagick
    elif command -v yum &> /dev/null; then
        sudo yum install -y ImageMagick
    elif command -v brew &> /dev/null; then
        brew install imagemagick
    else
        echo "Please install ImageMagick manually and run this script again."
        exit 1
    fi
fi

# Create icons directory if it doesn't exist
mkdir -p icons

# Base SVG file
SVG_FILE="icons/icon.svg"

if [ ! -f "$SVG_FILE" ]; then
    echo "SVG file not found: $SVG_FILE"
    exit 1
fi

echo "Generating PWA icons from $SVG_FILE..."

# Array of icon sizes needed for PWA
declare -a sizes=(
    "16:favicon-16x16.png"
    "32:favicon-32x32.png" 
    "57:apple-touch-icon-57x57.png"
    "60:apple-touch-icon-60x60.png"
    "72:icon-72x72.png"
    "76:apple-touch-icon-76x76.png"
    "96:icon-96x96.png"
    "114:apple-touch-icon-114x114.png"
    "120:apple-touch-icon-120x120.png"
    "128:icon-128x128.png"
    "144:icon-144x144.png"
    "152:apple-touch-icon-152x152.png"
    "180:apple-touch-icon.png"
    "192:icon-192x192.png"
    "384:icon-384x384.png"
    "512:icon-512x512.png"
)

# Generate each icon size
for size_info in "${sizes[@]}"; do
    IFS=':' read -r size filename <<< "$size_info"
    echo "Generating ${size}x${size} icon: $filename"
    
    convert "$SVG_FILE" -resize "${size}x${size}" "icons/$filename"
    
    if [ $? -eq 0 ]; then
        echo "✓ Generated icons/$filename"
    else
        echo "✗ Failed to generate icons/$filename"
    fi
done

# Generate ICO file for browsers
echo "Generating favicon.ico..."
convert "icons/favicon-32x32.png" "icons/favicon-16x16.png" "icons/favicon.ico"

if [ $? -eq 0 ]; then
    echo "✓ Generated icons/favicon.ico"
else
    echo "✗ Failed to generate icons/favicon.ico"
fi

echo ""
echo "Icon generation complete!"
echo "Generated icons:"
ls -la icons/*.png icons/*.ico 2>/dev/null | wc -l | xargs echo "Total files:"

echo ""
echo "You can now deploy your PWA with all required icons."
echo "Make sure to update your HTML head section with the appropriate icon links."