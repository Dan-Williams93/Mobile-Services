import tornado.httpserver
import tornado.ioloop
import tornado.web
import sqlite3
import sys

_db = sqlite3.connect('assignment.db')
_cursor = _db.cursor()

class itemRequestHandler(tornado.web.RequestHandler):

	def delete(self):
		_cursor.execute("DROP TABLE IF EXISTS items")
		_cursor.execute("CREATE TABLE items (name VARCHAR(255), price REAL, quantity INT, UNIQUE (name))")
		_cursor.execute("INSERT INTO items VALUES ('plum', '0.5', '10')")
		_cursor.execute("INSERT INTO items VALUES ('coffee', '1.5', '10')")
		_db.commit()
		self.write('OK')
 
	def put(self, ID):
	
		price = self.get_argument("price", default=False)
		quantity = self.get_argument("quantity", default=False)
		
		if (price != False):
			#update record
			record = [float(price), int(ID)]
			_cursor.execute("UPDATE items SET price=? WHERE name=?", record)
			_db.commit()
			self.write('OK')
		
		if (quantity != False):
			#update record
			record = [int(quantity), int(ID)]
			_cursor.execute("UPDATE items SET quantity=? WHERE name=?", record)
			_db.commit()
			self.write('OK')
		 
	def get(self, ID):
	
		price = self.get_argument("price", default=False)
		quantity = self.get_argument("quantity", default=False)
		value = self.get_argument("value", default=False)
		name = [ID]
				
		
		if (price):
			_cursor.execute("SELECT price FROM items WHERE name=?", name)
			for row in _cursor:
				itemPrice = float(row[0])
				
			itemPrice = format(itemPrice, '.2f')	
			self.write(ID + " unit price: " + itemPrice)
			
		elif (quantity):
			_cursor.execute("SELECT quantity FROM items WHERE name=?", name)
			for row in _cursor:
				itemStock = str(row[0])		

			self.write(ID + " stock level: " + itemStock)
		
		elif (value):
			_cursor.execute("SELECT * FROM items WHERE name=?", name)
			for row in _cursor:
				itemPrice = str(row[1])
				itemStock = str(row[2])
				
			itemVale = format(float(itemPrice) * int(itemStock), '.2f')
			self.write(ID + " total stock value: " + str(itemStock))
		

application = tornado.web.Application([
 (r"/item/(plum|coffee)", itemRequestHandler),
 (r"/database", itemRequestHandler),
])

if __name__ == "__main__":
 http_server = tornado.httpserver.HTTPServer(application)
 http_server.listen(43210)
 tornado.ioloop.IOLoop.instance().start()
