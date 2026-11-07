from django.contrib import admin

from .models import MonitorConfig, Post

admin.site.register(Post)
admin.site.register(MonitorConfig)
