# Contributing to Rooster

Thank you for your interest in contributing to Rooster!

## Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 11+
- Android SDK 34

### Building
```bash
make build        # Build the project
make test         # Run tests
make lint         # Run lint checks
make clean        # Clean build artifacts
make uninstall    # Uninstall from connected device
```

### Project Structure
See [ARCHITECTURE.md](ARCHITECTURE.md) for a detailed overview of the codebase.

## How to Contribute

### Bug Reports
- Open an issue with steps to reproduce, expected vs actual behavior, and device/Android version info.

### Pull Requests
1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Run tests: `make test`
5. Run lint: `make lint`
6. Submit a pull request with a clear description

### Code Style
- Follow existing Kotlin conventions
- Use meaningful names for variables and functions
- Keep functions focused and short
- Use constants from `AppConstants` instead of magic numbers
- Follow the MVVM pattern: UI logic in ViewModels, business logic in UseCases

### Architecture Guidelines
- New features should use the ViewModel + UseCase + Repository pattern
- Database access goes through `Repository` classes, never directly from Activities
- Use Hilt for dependency injection
- Background work uses WorkManager (except alarms which use `AlarmManager`)

## Bounties

You can find bounties for specific issues on [Bount.ing](https://bount.ing).

## License

By contributing, you agree that your contributions will be licensed under the GPLv3 License.
