#!/bin/bash

# Clara PWA Deployment Script
# This script sets up and deploys the complete Clara PWA

set -e  # Exit on any error

echo "🚀 Clara PWA Deployment Script"
echo "================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Firebase CLI is installed
check_firebase_cli() {
    if ! command -v firebase &> /dev/null; then
        print_error "Firebase CLI is not installed. Installing..."
        npm install -g firebase-tools
        print_success "Firebase CLI installed"
    else
        print_success "Firebase CLI found"
    fi
}

# Check if user is logged into Firebase
check_firebase_auth() {
    if ! firebase projects:list &> /dev/null; then
        print_warning "Not logged into Firebase. Please log in..."
        firebase login
    else
        print_success "Firebase authentication confirmed"
    fi
}

# Generate PWA icons
generate_icons() {
    print_status "Generating PWA icons..."
    
    if [ -f "generate-icons.sh" ]; then
        chmod +x generate-icons.sh
        ./generate-icons.sh
        print_success "PWA icons generated"
    else
        print_warning "Icon generation script not found, skipping..."
    fi
}

# Deploy Firestore rules
deploy_firestore_rules() {
    print_status "Deploying Firestore security rules..."
    
    if [ -f "firestore-complete.rules" ]; then
        # Copy rules to firebase.json location
        cp firestore-complete.rules firestore.rules
        
        firebase deploy --only firestore:rules
        print_success "Firestore rules deployed"
    else
        print_warning "Firestore rules file not found, skipping..."
    fi
}

# Deploy Cloud Functions
deploy_functions() {
    print_status "Deploying Cloud Functions..."
    
    if [ -d "functions" ]; then
        cd functions
        
        # Install dependencies if package.json exists
        if [ -f "package.json" ]; then
            print_status "Installing function dependencies..."
            npm install
        fi
        
        cd ..
        firebase deploy --only functions
        print_success "Cloud Functions deployed"
    else
        print_warning "Functions directory not found, skipping..."
    fi
}

# Deploy to Firebase Hosting
deploy_hosting() {
    print_status "Deploying to Firebase Hosting..."
    
    # Create firebase.json if it doesn't exist
    if [ ! -f "firebase.json" ]; then
        print_status "Creating firebase.json configuration..."
        cat > firebase.json << EOF
{
  "hosting": {
    "public": ".",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**",
      "**/*.sh",
      "**/*.md",
      "firestore-complete.rules"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ],
    "headers": [
      {
        "source": "/sw.js",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "no-cache"
          }
        ]
      },
      {
        "source": "/manifest.json",
        "headers": [
          {
            "key": "Content-Type",
            "value": "application/manifest+json"
          }
        ]
      }
    ]
  },
  "firestore": {
    "rules": "firestore.rules"
  },
  "functions": {
    "source": "functions"
  }
}
EOF
        print_success "firebase.json created"
    fi
    
    firebase deploy --only hosting
    print_success "App deployed to Firebase Hosting"
}

# Setup Firebase project
setup_firebase_project() {
    print_status "Setting up Firebase project..."
    
    # Check if .firebaserc exists
    if [ ! -f ".firebaserc" ]; then
        print_status "No Firebase project configured. Setting up..."
        firebase init --interactive
    else
        print_success "Firebase project already configured"
    fi
}

# Validate deployment
validate_deployment() {
    print_status "Validating deployment..."
    
    # Check if hosting URL is accessible
    if command -v curl &> /dev/null; then
        HOSTING_URL=$(firebase hosting:channel:list | grep -o 'https://[^[:space:]]*' | head -1)
        if [ ! -z "$HOSTING_URL" ]; then
            print_status "Testing deployment at $HOSTING_URL"
            if curl -s -f "$HOSTING_URL" > /dev/null; then
                print_success "Deployment is live and accessible!"
            else
                print_warning "Deployment may not be fully ready yet"
            fi
        fi
    fi
}

# Main deployment process
main() {
    echo ""
    print_status "Starting Clara PWA deployment process..."
    echo ""
    
    # Pre-deployment checks
    check_firebase_cli
    check_firebase_auth
    
    # Setup Firebase project if needed
    setup_firebase_project
    
    # Generate assets
    generate_icons
    
    # Deploy Firebase components
    deploy_firestore_rules
    deploy_functions
    deploy_hosting
    
    # Validate deployment
    validate_deployment
    
    echo ""
    print_success "🎉 Clara PWA deployment complete!"
    echo ""
    print_status "Next steps:"
    echo "1. Configure Firebase Web App credentials in js/config.js"
    echo "2. Setup push notifications following PUSH_NOTIFICATIONS_SETUP.md"
    echo "3. Test the app on different devices and browsers"
    echo "4. Configure custom domain if needed"
    echo ""
    print_status "Your app should be available at your Firebase hosting URL"
    firebase hosting:channel:list
}

# Handle script interruption
trap 'print_error "Deployment interrupted"; exit 1' INT

# Check if we're in the right directory
if [ ! -f "index.html" ]; then
    print_error "Please run this script from the webapp directory"
    exit 1
fi

# Run main deployment
main