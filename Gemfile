# Gemfile for n8n Monitor Android App
# This file manages Ruby dependencies for fastlane

source "https://rubygems.org"

gem "fastlane", "~> 2.217"

# Plugins
plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)