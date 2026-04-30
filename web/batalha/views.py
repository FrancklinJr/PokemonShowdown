from django.shortcuts import render

def index(request):
    return render(request, 'batalha/index.html')
