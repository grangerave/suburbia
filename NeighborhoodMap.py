# -*- coding: utf-8 -*-
"""
Created on Sun Oct  8 11:02:41 2017

@author: sasha


A dummy script to visualize the world generation to be implemented in the mod.
The idea is to be able to determine exactly what type of chunk to render using only the chunk coordinates.

General rules:
- A road takes up 1 chunk (road cross section: E#\__________/#E where E is sidewalk, _ is concrete slab, # grass, \ is concrete steps)
- A house + yard takes up 4 chunks
- Each house must have 2 road blocks in front of it
> Start with an infinite grid consisting of squares of 4 houses: call each one of these "tiles"
> Add coherent perturbations as follows:
a) remove 2 adjacent vertical roads
b) remove 2 adjacent horizontal roads
avoid having both perturbations on the same grid tile!!

To avoid duplicate perturbations
1) make adjacent perturbations identical (this results in larger regions with only parallel roads)
2) overlay this with higher frequency perturbations, to get a somewhat homogenous mix of perturbation scale
"""

import graphics as g
import numpy as np
padTop = 40
gridSize = 8
seed = 34167238491
px =0.46    #chance to remove 2 vertical roads
py=0.46     #chance to remove 2 horizontal roads
xscale=0
flatscale=1

def pt(x,y):
    return g.Point(x,y)

def grid():
    #draw grey lines along x and y coords
    for i in range(-ny+1,ny+1):
        xline = g.Line(pt(0,y0 + i*gridSize),pt(2*x0,y0 + i*gridSize))
        xline.setOutline(g.color_rgb(100,100,100))
        win.addItem(xline)
    for j in range(-nx,nx+1):
        yline = g.Line(pt(x0 + j*gridSize,0),pt(x0 + j*gridSize,2*y0))
        yline.setOutline(g.color_rgb(100,100,100))
        win.addItem(yline)
    print('drew grid...')
    
    
def chunkCoord(i,j):
    return pt(x0+i*gridSize,y0+j*gridSize)
    
def chunkCoordD(i,j,dx,dy):
    return pt(x0+i*gridSize+dx,y0+j*gridSize+dy)    
  
def chunk(i,j):
    #returns a blank indexed chunk
    blank = g.Rectangle(chunkCoord(i,j),chunkCoord(i+1,j+1))
    blank.setOutline("grey")
    return blank

def roadX(i,j):
    c = chunk(i,j)
    c.setFill("grey")
    c.draw(win)
    #add roadline
    
def woods(i,j):
    c = chunk(i,j)
    c.setFill(g.color_rgb(0,120,0))
    c.draw(win)
    '''
    #add tree
    t = g.Circle(chunkCoordD(i,j,gridSize/2,gridSize/2),gridSize/4)
    t.setFill(g.color_rgb(0,80,0))
    t.draw(win)
    '''
def Houses0 (i,j):
    #i,j in blocks
    #no roads removed: draw 4 houses as usual
    c1 = g.Circle(pt(x0+(i*5+2)*gridSize,y0 + (j*5+2)*gridSize),gridSize*0.5)
    c1.setFill("yellow")
    c1.draw(win)
    c2 = c1.clone()
    c2.move(2*gridSize,0)
    c2.draw(win)
    c3 = c1.clone()
    c3.move(0,2*gridSize)
    c3.draw(win)
    c4 = c1.clone()
    c4.move(2*gridSize,2*gridSize)
    c4.draw(win)
    
def HousesY (i,j,left,right):
    #horizontal roads removed
    #add more vertical houses
    #i,j in blocks
    if(left):
        c1 = g.Circle(pt(x0+(i*5+2)*gridSize,y0 + (j*5+1)*gridSize),gridSize*0.5)
        c1.setFill("yellow")
        c1.draw(win)
        c3 = c1.clone()
        c3.move(0,2*gridSize)
        c3.draw(win)
        c5 = c1.clone()
        c5.move(0,4*gridSize)
        c5.draw(win)
    if(right):
        c2 = g.Circle(pt(x0+(i*5+4)*gridSize,y0 + (j*5+1)*gridSize),gridSize*0.5)
        c2.setFill("yellow")
        c2.draw(win)
        c4 = c2.clone()
        c4.move(0,2*gridSize)
        c4.draw(win)
        c6 = c2.clone()
        c6.move(0,4*gridSize)
        c6.draw(win)
         
def HousesX (i,j,top,bottom):
    #vertical roads removed
    #add more horizontal houses
    #i,j in blocks
    if(top):
        c1 = g.Circle(pt(x0+(i*5+1)*gridSize,y0 + (j*5+2)*gridSize),gridSize*0.5)
        c1.setFill("yellow")
        c1.draw(win)
        c2 = c1.clone()
        c2.move(2*gridSize,0)
        c2.draw(win)
        c5 = c1.clone()
        c5.move(4*gridSize,0)
        c5.draw(win)
    if(bottom):
        c3 = g.Circle(pt(x0+(i*5+1)*gridSize,y0 + (j*5+4)*gridSize),gridSize*0.5)
        c3.setFill("yellow")
        c3.draw(win)
        c4 = c3.clone()
        c4.move(2*gridSize,0)
        c4.draw(win)
        c6 = c3.clone()
        c6.move(4*gridSize,0)
        c6.draw(win)         
  

win = g.GraphWin('Map', 1200, 720, autoflush=False) # give title and dimensions
#figure out widths
x0 = win.getWidth()/2
y0 = win.getHeight()/2
#how many chunks?
nx = int(x0//gridSize)
ny = int(y0//gridSize)
#how many blocks?
bx = int(x0//(10*gridSize))+1
by = int(y0//(10*gridSize))+1

print('Showing {' + str(2*nx) + "," + str(2*ny) + "} chunks, {"+ str(2*bx) + "," + str(2*by) + "} blocks")

#draw grid
grid()

#rng stuff
#seed the rng
#np.random.seed(seed)
coarsemap = np.random.rand(2*bx,2*by)
xmap = (0)*np.random.rand(4*bx,4*by)
for i in range(4*bx):
    for j in range(4*by):
        xmap[i,j]+=flatscale*coarsemap[i//2,j//2]

ymap = 1-xmap


#draw ALL chunks
for i in range(-nx,nx):
    for j in range(-ny,ny):
        if(i%5==0): #yroad
            if(j%5==0): #intersection
                roadX(i,j)
            #keep this road? VERTICAL ROAD
            elif(ymap[i//5,j//5]>py):
                roadX(i,j)
            else:
                #print('removed V:' + str(i//10) + "," +  str(j//10))
                woods(i,j)
        elif(j%5==0): #xroad
            #remove this road? HORIZONTAL ROAD
            if(xmap[i//5,j//5]>px):
                roadX(i,j)
            else:
                woods(i,j)
        else:
            woods(i,j)
 
      
#draw houses
doHouses = True
if doHouses:
    for i in range(-2*bx,2*bx):
        for j in range(-2*by,2*by):
            #print(str(i)+","+str(j))
    
            if(i%2==0 and ymap[i,j]<py): #removed vertical roads
                if((j+1)%2==0 and xmap[i,2*((j+1)//2)]<px):
                    HousesX(i,j,True,False)#nothing on bottom
                elif((j-1)%2==0 and xmap[i,2*((j-1)//2)]<px):
                    HousesX(i,j,False,True)#nothing on top
                else:
                    HousesX(i,j,True,True)
            elif(j%2==0 and xmap[i,j]<px): #removed horizontal roads
                #draw housesY
                if((i-1)%2==0 and ymap[2*((i-1)//2),j]<py):
                    HousesY(i,j,False,True)
                elif((i+1)%2==0 and ymap[2*((i+1)//2),j]<py):
                    HousesY(i,j,True,False)
                else:
                    HousesY(i,j,True,True)
            #else:
                #Houses0(i,j)

            

#draw coordinate origins
xcoord = g.Line(pt(x0,y0),pt(x0 + 32,y0))
xcoord.setArrow("last")
xcoord.setOutline("red")
ycoord = g.Line(pt(x0,y0),pt(x0,y0 + 32))
ycoord.setArrow("last")
ycoord.setOutline("blue")

xcoord.draw(win)
ycoord.draw(win)
#win.addItem(xcoord)
#win.addItem(ycoord)

#draw grid



win.update()
print('success')



message = g.Text(g.Point(x0, 10), 'Click anywhere to quit.')
message.setOutline("red")
message.draw(win)
win.getMouse()
win.close()
