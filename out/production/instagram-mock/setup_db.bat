@echo off
setlocal enabledelayedexpansion

:: Configuration
set PG_USER=postgres
set PG_PASSWORD=password
set DB_NAME=messaging_app
set SCHEMA_FILE=db\schema.sql
set SEED_DATA_FILE=db\seed_data.sql

:: Check if PostgreSQL is in PATH
where psql >nul 2>&1
if %errorlevel% neq 0 (
    echo PostgreSQL tools (psql) are not found in your PATH.
    echo Please add PostgreSQL's "bin" directory to your PATH and retry.
    pause
    exit /b
)

:: Create the database
echo Creating database %DB_NAME%...
psql -U %PG_USER% -c "CREATE DATABASE %DB_NAME%;" >nul 2>&1
if %errorlevel% neq 0 (
    echo Failed to create database. It might already exist or you lack permissions.
    echo Check the error above and retry.
    pause
    exit /b
)

:: Import schema
echo Importing schema from %SCHEMA_FILE%...
psql -U %PG_USER% -d %DB_NAME% -f %SCHEMA_FILE%
if %errorlevel% neq 0 (
    echo Failed to import schema. Check the file path and database permissions.
    pause
    exit /b
)

:: Import seed data (optional)
if exist %SEED_DATA_FILE% (
    echo Importing seed data from %SEED_DATA_FILE%...
    psql -U %PG_USER% -d %DB_NAME% -f %SEED_DATA_FILE%
    if %errorlevel% neq 0 (
        echo Failed to import seed data. Check the file path and database permissions.
        pause
        exit /b
    )
)

echo Database setup complete!
pause
